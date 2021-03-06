package com.gmi.nordborglab.browser.server.domain.pages;

import com.gmi.nordborglab.browser.server.data.es.ESFacet;
import com.gmi.nordborglab.browser.server.domain.observation.Experiment;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

public class ExperimentPage extends PageImpl<Experiment> {

    private List<ESFacet> facets;

    public ExperimentPage() {
        super(new ArrayList<Experiment>(), new PageRequest(0, 1), 0);
    }

    public ExperimentPage(List<Experiment> content, Pageable pageable, long total, List<ESFacet> facets) {
        super(content, pageable, total);
        this.facets = facets;
    }

    public ExperimentPage(List<Experiment> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    //NOTE required because there is a hasContent() getter ->  https://code.google.com/p/google-web-toolkit/issues/detail?id=6587
    public List<Experiment> getContents() {
        return getContent();
    }


    public List<ESFacet> getFacets() {
        return facets;
    }
}
