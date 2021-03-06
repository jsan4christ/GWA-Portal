package com.gmi.nordborglab.browser.client.mvp;

import com.gmi.nordborglab.browser.client.events.DisplayNotificationEvent;
import com.gmi.nordborglab.browser.client.events.LoadUserNotificationEvent;
import com.gmi.nordborglab.browser.client.events.LoadingIndicatorEvent;
import com.gmi.nordborglab.browser.client.events.UserChangeEvent;
import com.gmi.nordborglab.browser.client.place.NameTokens;
import com.gmi.nordborglab.browser.client.security.CurrentUser;
import com.gmi.nordborglab.browser.shared.proxy.AppUserProxy;
import com.gmi.nordborglab.browser.shared.proxy.UserNotificationProxy;
import com.gmi.nordborglab.browser.shared.service.AppUserFactory;
import com.gmi.nordborglab.browser.shared.service.CustomRequestFactory;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ProxyStandard;
import com.gwtplatform.mvp.client.presenter.slots.NestedSlot;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.gwtbootstrap3.client.ui.constants.AlertType;

import javax.annotation.Nullable;
import java.util.List;

public class ApplicationPresenter extends
        Presenter<ApplicationPresenter.MyView, ApplicationPresenter.MyProxy> implements ApplicationUiHandlers {

    public interface MyView extends View, HasUiHandlers<ApplicationUiHandlers> {

        String getUserData();

        void showUserInfo(AppUserProxy user);

        void setActiveNavigationItem(MENU menu);

        void showNotification(String caption, String message, AlertType type,
                              int duration, boolean isDismissable);

        void showLoadingIndicator(boolean show, String text);

        void refreshNotifications(List<UserNotificationProxy> notifications, boolean isRead);

        void resetNotificationBubble();
    }

    public static final NestedSlot SLOT_MAIN_CONTENT = new NestedSlot();

    @ProxyStandard
    public interface MyProxy extends Proxy<ApplicationPresenter> {

    }

    private List<UserNotificationProxy> userNotificationList;

    private final AppUserFactory appUserFactory;
    protected final CurrentUser currentUser;
    private final PlaceManager placeManager;
    private final CustomRequestFactory rf;
    private boolean isRead = false;

    public enum MENU {HOME, DIVERSITY, GERMPLASM, GENOTYPE;}

    @Inject
    public ApplicationPresenter(final EventBus eventBus, final MyView view,
                                final MyProxy proxy,
                                final AppUserFactory appUserFactory, final CurrentUser currentUser,
                                final PlaceManager placeManager,
                                final CustomRequestFactory rf) {
        super(eventBus, view, proxy, RevealType.RootLayout);
        getView().setUiHandlers(this);
        this.appUserFactory = appUserFactory;
        this.currentUser = currentUser;
        this.placeManager = placeManager;
        this.rf = rf;
    }


    @Override
    protected void onBind() {
        super.onBind();
        registerHandler(getEventBus().addHandler(DisplayNotificationEvent.getType(), new DisplayNotificationEvent.DisplayNotificationHandler() {

            @Override
            public void onDisplayNotifcation(DisplayNotificationEvent event) {
                if (!event.isAlreadyDisplayed()) {
                    getView().showNotification(event.getCaption(), event.getMessage(), event.getAlertType(), event.getDuration(), event.isDismissable());
                    event.setAlreadyDisplayed();
                }
            }
        }));

        registerHandler(getEventBus().addHandler(LoadingIndicatorEvent.getType(), new LoadingIndicatorEvent.LoadingIndicatorHandler() {

            @Override
            public void onProcessLoadingIndicator(LoadingIndicatorEvent event) {
                getView().showLoadingIndicator(event.getShow(), event.getText());
            }
        }));
        registerHandler(getEventBus().addHandler(LoadUserNotificationEvent.TYPE, new LoadUserNotificationEvent.Handler() {
            @Override
            public void onLoaduserNotifications(LoadUserNotificationEvent event) {
                refreshUserNotifications();
            }
        }));

        registerHandler(getEventBus().addHandler(UserChangeEvent.TYPE, new UserChangeEvent.Handler() {
            @Override
            public void onChanged(UserChangeEvent event) {
                getView().showUserInfo(currentUser.getAppUser());
            }
        }));
    }

    private void refreshUserNotifications() {
        rf.helperRequest().getUserNotifications(10).fire(new Receiver<List<UserNotificationProxy>>() {
            @Override
            public void onSuccess(List<UserNotificationProxy> response) {
                isRead = false;
                userNotificationList = response;
                getView().refreshNotifications(response, isRead);
            }
        });
    }

    private MENU getParentMenuFromRequest(PlaceRequest request) {
        MENU menu = MENU.HOME;
        if (request.matchesNameToken(NameTokens.experiment) ||
                request.matchesNameToken(NameTokens.experiments) ||
                request.matchesNameToken(NameTokens.phenotypes) ||
                request.matchesNameToken(NameTokens.phenotype) ||
                request.matchesNameToken(NameTokens.studylist) ||
                request.matchesNameToken(NameTokens.study) ||
                request.matchesNameToken(NameTokens.studygwas) ||
                request.matchesNameToken(NameTokens.studyoverview) ||
                request.matchesNameToken(NameTokens.phenotypeoverview) ||
                request.matchesNameToken(NameTokens.traitontology) ||
                request.matchesNameToken(NameTokens.gwasViewer) ||
                request.matchesNameToken(NameTokens.metaAnalysisGenes) ||
                request.matchesNameToken(NameTokens.metaAnalysisTopResults) ||
                request.matchesNameToken(NameTokens.candidateGeneList) ||
                request.matchesNameToken(NameTokens.candidateGeneListDetail) ||
                request.matchesNameToken(NameTokens.experimentsEnrichments) ||
                request.matchesNameToken(NameTokens.phenotypeEnrichments) ||
                request.matchesNameToken(NameTokens.studyEnrichments) ||
                request.matchesNameToken(NameTokens.snps)
                )
            menu = MENU.DIVERSITY;
        else if (request.matchesNameToken(NameTokens.taxonomies) ||
                request.matchesNameToken(NameTokens.taxonomy) ||
                request.matchesNameToken(NameTokens.passports) ||
                request.matchesNameToken(NameTokens.passport) ||
                request.matchesNameToken(NameTokens.stock)
                )
            menu = MENU.GERMPLASM;
        else if (request.matchesNameToken(NameTokens.genomebrowser) ||
                request.matchesNameToken(NameTokens.snpviewer)) {
            menu = MENU.GENOTYPE;
        }
        return menu;
    }

    @Override
    protected void onReset() {
        super.onReset();
        if (userNotificationList == null)
            userNotificationList = currentUser.getAppData().getUserNotificationList();
        getView().showUserInfo(currentUser.getAppUser());
        PlaceRequest request = placeManager.getCurrentPlaceRequest();
        getView().setActiveNavigationItem(getParentMenuFromRequest(request));
        getView().refreshNotifications(userNotificationList, isRead);
    }

    @Override
    protected void onUnbind() {
        super.onUnbind();
    }

    @Override
    public void onOpenAccountInfo() {
        if (userNotificationList == null)
            return;
        if (Iterables.find(userNotificationList, new Predicate<UserNotificationProxy>() {
            @Override
            public boolean apply(@Nullable UserNotificationProxy input) {
                return input != null && !input.isRead();
            }
        }, null) != null) {
            currentUser.updateNotificationCheckDate();
        }
        getView().resetNotificationBubble();
    }

    @Override
    public void onCloseAccountInfo() {
        if (userNotificationList == null)
            return;
        isRead = true;
        getView().refreshNotifications(userNotificationList, isRead);
    }
}
