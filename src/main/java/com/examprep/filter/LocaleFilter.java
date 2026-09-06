package com.examprep.filter;

import com.examprep.i18n.LocaleSupport;
import com.examprep.i18n.Messages;
import com.examprep.model.AppLocale;
import com.examprep.model.User;
import com.examprep.util.WebUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.jstl.core.Config;
import jakarta.servlet.jsp.jstl.fmt.LocalizationContext;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Resolves Tagalog (default) or English from the locale cookie, then the signed-in user.
 */
public class LocaleFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        try {
            AppLocale locale = resolve(req);
            LocaleSupport.setCurrent(locale);
            req.setAttribute(LocaleSupport.REQUEST_ATTR, locale);
            req.setAttribute(LocaleSupport.CURRENT_PATH_ATTR, LocaleSupport.currentRequestPath(req));

            ResourceBundle bundle = Messages.bundle(locale);
            Config.set(req, Config.FMT_LOCALE, locale.toBundleLocale());
            Config.set(req, Config.FMT_FALLBACK_LOCALE, AppLocale.EN.toBundleLocale());
            Config.set(req, Config.FMT_LOCALIZATION_CONTEXT,
                    new LocalizationContext(bundle, locale.toBundleLocale()));

            chain.doFilter(request, response);
        } finally {
            LocaleSupport.clear();
        }
    }

    private static AppLocale resolve(HttpServletRequest request) {
        AppLocale fromCookie = LocaleSupport.fromCookie(request);
        if (fromCookie != null) {
            return fromCookie;
        }
        User user = WebUtil.getCurrentUser(request);
        if (user != null && user.getLocale() != null) {
            return user.getLocale();
        }
        return AppLocale.DEFAULT;
    }
}
