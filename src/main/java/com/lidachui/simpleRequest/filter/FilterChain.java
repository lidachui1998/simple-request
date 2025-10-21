package com.lidachui.simpleRequest.filter;

import com.lidachui.simpleRequest.constants.FilterPhase;
import com.lidachui.simpleRequest.resolver.Request;
import com.lidachui.simpleRequest.resolver.RequestContext;
import com.lidachui.simpleRequest.resolver.Response;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 过滤器链
 *
 * @author lihuijie
 * @date 2025/01/22
 */
@Slf4j
public class FilterChain {
    private final List<AbstractRequestFilter> filters;
    private final Map<FilterPhase, Integer> phasePositions;

    public FilterChain(List<AbstractRequestFilter> filters) {
        this.filters = filters;
        this.phasePositions = new HashMap<>();
    }

    public void doFilter(
            Request request, Response response, RequestContext context, FilterPhase phase) {
        doFilter(request, response, context, phase, null);
    }

    public void doFilter(
            Request request, Response response, RequestContext context, FilterPhase phase, Exception exception) {
        int position = phasePositions.getOrDefault(phase, 0);

        if (position < filters.size()) {
            AbstractRequestFilter currentFilter = filters.get(position);
            try {
                currentFilter.setRequestContext(context);
                executeFilter(currentFilter, request, response, phase, exception);
            } catch (Exception e) {
                log.error(
                        "Error executing {} filter: {}",
                        phase,
                        currentFilter.getClass().getName(),
                        e);
            }
            phasePositions.put(phase, position + 1);
        }
    }

    private void executeFilter(
            AbstractRequestFilter filter, Request request, Response response, FilterPhase phase, Exception exception) {
        switch (phase) {
            case PRE_HANDLE:
                filter.preHandle(request);
                break;
            case AFTER_COMPLETION:
                filter.afterCompletion(request, response);
                break;
            case ERROR:
                filter.error(request, response, exception);
                break;
            default:
                break;
        }
    }
}
