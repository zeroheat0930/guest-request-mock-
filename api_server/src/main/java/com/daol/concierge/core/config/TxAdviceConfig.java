package com.daol.concierge.core.config;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.CompositeTransactionAttributeSource;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Aspect
@Configuration
public class TxAdviceConfig {

	private static final String AOP_POINTCUT_EXPRESSION = "execution(* com.daol.concierge..service.*Service.*(..))";
	private static final String[] TX_METHOD_NAME = {
		"insert*",
		"update*",
		"delete*",
		"save*",
		"proc*"
	};

	private final PlatformTransactionManager transactionManager;

	@Bean
	public TransactionInterceptor txAdvice() {
		List<MatchAlwaysTransactionAttributeSource> sourceList = new ArrayList<>();

		for (String methodName : TX_METHOD_NAME) {
			MatchAlwaysTransactionAttributeSource source = new MatchAlwaysTransactionAttributeSource();
			RuleBasedTransactionAttribute attr = new RuleBasedTransactionAttribute();
			attr.setName(methodName);
			attr.setRollbackRules(Collections.singletonList(new RollbackRuleAttribute(Exception.class)));
			source.setTransactionAttribute(attr);
			sourceList.add(source);
		}

		return new TransactionInterceptor(transactionManager,
				new CompositeTransactionAttributeSource(sourceList.toArray(new MatchAlwaysTransactionAttributeSource[0])));
	}

	@Bean
	public Advisor txAdviceAdvisor() {
		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(AOP_POINTCUT_EXPRESSION);
		return new DefaultPointcutAdvisor(pointcut, txAdvice());
	}
}
