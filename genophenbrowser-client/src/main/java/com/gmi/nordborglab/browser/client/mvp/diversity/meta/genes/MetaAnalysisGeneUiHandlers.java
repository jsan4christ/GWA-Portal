package com.gmi.nordborglab.browser.client.mvp.diversity.meta.genes;

import com.gmi.nordborglab.browser.client.manager.SearchManager;
import com.gmi.nordborglab.browser.shared.proxy.AssociationProxy;
import com.gmi.nordborglab.browser.shared.proxy.MetaAnalysisProxy;
import com.gwtplatform.mvp.client.UiHandlers;

/**
 * Created with IntelliJ IDEA.
 * User: uemit.seren
 * Date: 5/15/13
 * Time: 1:40 PM
 * To change this template use File | Settings | File Templates.
 */
public interface MetaAnalysisGeneUiHandlers extends UiHandlers {

    void onSearchForGene(String request, final SearchManager.SearchCallback callback);

    void onSelectGene(String gene);

    void onSelectMetaAnalysis(MetaAnalysisProxy metaAnalysis);

    void onChangeRange(int lowerLimit, int upperLimit);

    void onSelectVisualization(MetaAnalysisGenePresenter.VIZ_TYPE vizType);

    void onSelectAssociation(AssociationProxy associationProxy);

}
