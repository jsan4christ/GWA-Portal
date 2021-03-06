package com.gmi.nordborglab.browser.client.bootstrap;

import com.eemi.gwt.tour.client.GwtTour;
import com.gmi.nordborglab.browser.client.events.DisplayNotificationEvent;
import com.gmi.nordborglab.browser.client.events.UserChangeEvent;
import com.gmi.nordborglab.browser.client.place.GoogleAnalyticsManager;
import com.gmi.nordborglab.browser.client.security.CurrentUser;
import com.gmi.nordborglab.browser.client.util.ParallelRunnable;
import com.gmi.nordborglab.browser.client.util.ParentCallback;
import com.gmi.nordborglab.browser.shared.exceptions.SessionTimeOutException;
import com.gmi.nordborglab.browser.shared.proxy.AppDataProxy;
import com.gmi.nordborglab.browser.shared.proxy.AppUserProxy;
import com.gmi.nordborglab.browser.shared.service.AppUserFactory;
import com.gmi.nordborglab.browser.shared.service.CustomRequestFactory;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.maps.client.LoadApi;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.inject.Inject;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryLogHandler;
import com.google.web.bindery.requestfactory.shared.LoggingRequest;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.googlecode.gwt.charts.client.ChartLoader;
import com.googlecode.gwt.charts.client.ChartPackage;
import com.gwtplatform.mvp.client.Bootstrapper;
import com.gwtplatform.mvp.client.proxy.PlaceManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by uemit.seren on 11.01.14.
 */
public class BootstrapperImpl implements Bootstrapper {
    private final PlaceManager placeManager;
    private final CurrentUser currentUser;
    private final CustomRequestFactory rf;
    private final AppUserFactory appUserFactory;
    private final GoogleAnalyticsManager analyticsManager;
    private final EventBus eventBus;

    @Inject
    public BootstrapperImpl(PlaceManager placeManager, CurrentUser currentUser,
                            CustomRequestFactory rf, AppUserFactory appUserFactory,
                            GoogleAnalyticsManager analyticsManager, EventBus eventBus

    ) {
        this.placeManager = placeManager;
        this.currentUser = currentUser;
        this.rf = rf;
        this.eventBus = eventBus;
        this.appUserFactory = appUserFactory;
        this.analyticsManager = analyticsManager;
    }

    @Override
    public void onBootstrap() {
        analyticsManager.create(getGATrackingId()).go();
        // Add remote logging handler
        RequestFactoryLogHandler.LoggingRequestProvider provider = new RequestFactoryLogHandler.LoggingRequestProvider() {
            public LoggingRequest getLoggingRequest() {
                return rf.loggingRequest();
            }
        };
        Logger.getLogger("").addHandler(new RequestFactoryLogHandler(provider, Level.WARNING, new ArrayList<String>()));
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {

            @Override
            public void onUncaughtException(Throwable e) {
                if (e instanceof SessionTimeOutException) {
                    currentUser.setAppUser(null);
                    eventBus.fireEvent(new UserChangeEvent(null));
                    DisplayNotificationEvent.fireWarning(eventBus, "Authentication error", "Your session timed out. Log In again.");
                    return;
                } else {
                    // TODO share exception between client and server
                    if (e.getMessage().equals("Exception caught: Server Error: Access is denied")) {
                        if (currentUser.isLoggedIn()) {
                            currentUser.setAppUser(null);
                            eventBus.fireEvent(new UserChangeEvent(null));
                            DisplayNotificationEvent.fireWarning(eventBus, "Authentication error", "Your session timed out. Log In again.");
                            return;
                        }
                    }
                }
                Logger logger = Logger.getLogger("uncaught");
                logger.log(Level.SEVERE, "Uncaught Exception: " + e.getMessage(), e);
                analyticsManager.sendError("Uncaught", e.getMessage(), true);
            }
        });

        GwtTour.load();
        initUserData();
        //final ParallelRunnable visualizationRunnable = new ParallelRunnable();
        final ParallelRunnable rfRunnalbe = new ParallelRunnable();
        final ParallelRunnable mapsRunnable = new ParallelRunnable();
        final ParallelRunnable chartsRunnable = new ParallelRunnable();
        Receiver<AppDataProxy> receiver = new Receiver<AppDataProxy>() {

            @Override
            public void onSuccess(AppDataProxy response) {
                currentUser.setAppData(response);
                rfRunnalbe.run();
            }
        };

        ParentCallback parentCallback = new ParentCallback(chartsRunnable, rfRunnalbe, mapsRunnable) {

            @Override
            protected void handleSuccess() {
                RootPanel.getBodyElement().addClassName("loaded");
                placeManager.revealCurrentPlace();
            }
        };
        /* FIXME https://code.google.com/p/gwt-charts/issues/detail?id=53 */
        ChartLoader chartLoader = new ChartLoader(ChartPackage.CORECHART, ChartPackage.ORGCHART, ChartPackage.GEOCHART, ChartPackage.MOTIONCHART);
        chartLoader.loadApi(chartsRunnable);

        // load all the libs for use in the maps
        ArrayList<LoadApi.LoadLibrary> loadLibraries = new ArrayList<LoadApi.LoadLibrary>();
        loadLibraries.add(LoadApi.LoadLibrary.DRAWING);
        loadLibraries.add(LoadApi.LoadLibrary.GEOMETRY);
        loadLibraries.add(LoadApi.LoadLibrary.PANORAMIO);
        loadLibraries.add(LoadApi.LoadLibrary.PLACES);
        loadLibraries.add(LoadApi.LoadLibrary.WEATHER);
        LoadApi.go(mapsRunnable, loadLibraries, false);


        if (currentUser.getAppData() == null) {
            rf.helperRequest().getAppData().with("news.author").fire(receiver);
        } else {
            rfRunnalbe.run();
        }

    }

    private void initUserData() {
        String userData = getUserData();
        if (userData != null) {
            try {
                AutoBean<AppUserProxy> userBean = AutoBeanCodex.decode(appUserFactory, AppUserProxy.class, userData);
                currentUser.setAppUser(userBean.as());
            } catch (Exception e) {
                Logger logger = Logger.getLogger("");
                logger.log(Level.SEVERE, "Autobean decoding", e);
            }
        } else {
            currentUser.setAppUser(null);
        }
        analyticsManager.setLoggedIn(currentUser.isLoggedIn());
    }

    public static String getContactEmail() {
        Dictionary data = Dictionary.getDictionary("appData");
        return data.get("contactEmail");
    }

    public static LinkedHashSet<String> getChromosomes() {
        Dictionary data = Dictionary.getDictionary("appData");
        return Sets.newLinkedHashSet(Splitter.on(",").split(data.get("chromosomes")));

    }

    protected String getGATrackingId() {
        try {
            Dictionary data = Dictionary.getDictionary("appData");
            if (data != null) {
                return data.get("gaTrackingId");
            }
        } catch (Exception e) {
        }
        return null;
    }

    protected String getUserData() {
        String user = null;
        try {
            Dictionary data = Dictionary.getDictionary("userData");
            if (data != null) {
                user = data.get("user");
            }
        } catch (Exception e) {
        }
        return user;
    }
}
