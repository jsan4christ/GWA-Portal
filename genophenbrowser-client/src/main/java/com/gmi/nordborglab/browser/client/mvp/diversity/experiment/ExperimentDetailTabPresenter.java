package com.gmi.nordborglab.browser.client.mvp.diversity.experiment;

import com.gmi.nordborglab.browser.client.mvp.diversity.DiversityPresenter;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.ChangeTabHandler;
import com.gwtplatform.mvp.client.RequestTabsHandler;
import com.gwtplatform.mvp.client.TabContainerPresenter;
import com.gwtplatform.mvp.client.TabView;
import com.gwtplatform.mvp.client.annotations.ChangeTab;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.RequestTabs;
import com.gwtplatform.mvp.client.presenter.slots.NestedSlot;
import com.gwtplatform.mvp.client.proxy.Proxy;

public class ExperimentDetailTabPresenter
        extends
        TabContainerPresenter<ExperimentDetailTabPresenter.MyView, ExperimentDetailTabPresenter.MyProxy> {

    public interface MyView extends TabView {
    }

    @ProxyCodeSplit
    public interface MyProxy extends Proxy<ExperimentDetailTabPresenter> {
    }

    @RequestTabs
    public static final Type<RequestTabsHandler> TYPE_RequestTabs = new Type<RequestTabsHandler>();

    @ChangeTab
    public static final Type<ChangeTabHandler> TYPE_ChangeTab = new Type<ChangeTabHandler>();


    public static final NestedSlot SLOT_CONTENT = new NestedSlot();


    @Inject
    public ExperimentDetailTabPresenter(final EventBus eventBus, final MyView view,
                                        final MyProxy proxy) {
        super(eventBus, view, proxy, SLOT_CONTENT, TYPE_RequestTabs, TYPE_ChangeTab, DiversityPresenter.SLOT_CONTENT);
    }
}
