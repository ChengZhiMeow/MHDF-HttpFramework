package cn.chengzhiya.mhdfhttpframework.server.entity;

import cn.chengzhiya.mhdfhttpframework.server.enums.FilterType;
import lombok.Getter;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import javax.servlet.Filter;

@Getter
public final class FilterConfig {
    private final String name;
    private FilterType type;
    private Filter filter;
    private String urlPattern;

    public FilterConfig(String name) {
        this.name = name;
    }

    public FilterConfig(String name, Filter filter) {
        this(name);
        setFilter(filter);
    }

    public FilterConfig(String name, String urlPattern) {
        this(name);
        setUrlPattern(urlPattern);
    }

    public void setFilter(Filter filter) {
        this.type = FilterType.ENTITY;
        this.filter = filter;
    }

    public void setUrlPattern(String urlPattern) {
        this.type = FilterType.URL;
        this.urlPattern = urlPattern;
    }

    public FilterDef toFilterDef() {
        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName(name);
        filterDef.setFilter(filter);

        return filterDef;
    }

    public FilterMap toFilterMap() {
        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName(getName());
        filterMap.addURLPattern(urlPattern);

        return filterMap;
    }
}
