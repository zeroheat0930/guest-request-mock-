package com.daol.concierge.core.api;

import com.daol.concierge.core.vo.PageableVO;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class Responses {

    public static class MapResponse extends ApiResponse {
        @JsonProperty("map")
        private Map<String, Object> map;

        public Map<String, Object> getMap() { return map; }
        public void setMap(Map<String, Object> map) { this.map = map; }

        public static MapResponse of(Map<String, Object> data) {
            MapResponse r = new MapResponse();
            r.setStatus(ApiStatus.SUCCESS.getCode());
            r.setMessage("SUCCESS");
            r.setMap(data);
            return r;
        }
    }

    public static class ListResponse extends ApiResponse {
        @JsonProperty("list")
        private List<?> list;

        @JsonProperty("page")
        private PageableVO page;

        public List<?> getList() { return list; }
        public void setList(List<?> list) { this.list = list; }
        public PageableVO getPage() { return page; }
        public void setPage(PageableVO page) { this.page = page; }

        public static ListResponse of(List<?> list) {
            ListResponse r = new ListResponse();
            r.setStatus(ApiStatus.SUCCESS.getCode());
            r.setMessage("SUCCESS");
            r.setList(list);
            r.setPage(PageableVO.of(0, (long) list.size(), 0, 0));
            return r;
        }
    }

    public static class PageResponse extends ApiResponse {
        @JsonProperty("list")
        private List<?> list;

        @JsonProperty("page")
        private PageableVO page;

        public List<?> getList() { return list; }
        public void setList(List<?> list) { this.list = list; }
        public PageableVO getPage() { return page; }
        public void setPage(PageableVO page) { this.page = page; }

        public static PageResponse of(org.springframework.data.domain.Page<?> page) {
            PageResponse r = new PageResponse();
            r.setStatus(ApiStatus.SUCCESS.getCode());
            r.setMessage("SUCCESS");
            r.setList(page.getContent());
            r.setPage(PageableVO.of(page));
            return r;
        }
    }
}
