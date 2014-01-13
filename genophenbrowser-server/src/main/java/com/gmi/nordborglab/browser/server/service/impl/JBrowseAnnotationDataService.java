package com.gmi.nordborglab.browser.server.service.impl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.gmi.nordborglab.browser.server.data.annotation.Gene;
import com.gmi.nordborglab.browser.server.data.annotation.GeneAnnotation;
import com.gmi.nordborglab.browser.server.data.annotation.Isoform;
import com.gmi.nordborglab.browser.server.data.annotation.JBrowseChrTrackData;
import com.gmi.nordborglab.browser.server.data.annotation.SNPAnnot;
import com.gmi.nordborglab.browser.server.data.annotation.nclist.HistogramMeta;
import com.gmi.nordborglab.browser.server.service.AnnotationDataService;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.visualization.datasource.base.TypeMismatchException;
import com.google.visualization.datasource.datatable.ColumnDescription;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.datatable.value.ValueType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: uemit.seren
 * Date: 3/4/13
 * Time: 4:10 PM
 * To change this template use File | Settings | File Templates.
 */

@Service("jbrowse")
public class JBrowseAnnotationDataService implements AnnotationDataService {


    private Map<String, JBrowseChrTrackData> map = Maps.newHashMap();
    private Map<String, GeneAnnotation> annotationMap;

    @Value("${JBROWSE.data_folder}")
    private String JBROWSE_DATA_FOLDER;

    @Value("${JBROWSE.track_folder}")
    private String JBROWSE_TRACK_FOLDER;

    @Value("${GENE.annotation_file}")
    private String GENE_ANNOTATION_FILE;

    @Override
    public List<Gene> getGenes(String chr, Long start, Long end, boolean isFeatures) {
        JBrowseChrTrackData jBrowseData = getJBrowseChrTrackData(chr);
        try {
            return jBrowseData.getGenes(start, end, isFeatures);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Isoform getGeneIsoform(String isoform) {
        String gene = Splitter.on('.').split(isoform).iterator().next();
        GeneAnnotation annotation = annotationMap.get(gene);
        return annotation.getIsoforms().get(isoform);
    }

    @Override
    public List<SNPAnnot> getSNPAnnotations(String chr, int[] positions) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Gene getGeneById(String id) {
        GeneAnnotation annotation = annotationMap.get(id);
        Gene gene = null;
        if (annotation != null) {
            gene = new Gene(annotation.getStart_pos(), annotation.getEnd_pos(), annotation.getStrand(), annotation.getName(), null);
        }
        return gene;
    }

    @Override
    public DataTable getGenomeStatData(String stats, String chr) {
        DataTable table = new DataTable();
        table.addColumn(new ColumnDescription("position", ValueType.NUMBER, "Position"));
        List<String> statList = Lists.newArrayList(Splitter.on(",").trimResults().omitEmptyStrings().split(stats));

        if (chr == null || chr.equalsIgnoreCase("null")) {
            chr = "Chr1";
        }
        if (statList.contains("genecount")) {
            table.addColumn(new ColumnDescription("genecount", ValueType.NUMBER, "# genes"));
            JBrowseChrTrackData trackData = getJBrowseChrTrackData(chr);
            Integer[] geneCount = trackData.getHistogramData();
            HistogramMeta meta = trackData.getHistogramMeta();
            for (int i = 0; i < geneCount.length; i++) {
                try {
                    table.addRowFromValues(i * meta.getBasesPerBin(), geneCount[i]);
                } catch (TypeMismatchException e) {

                }
            }
        } else {
        }
        return table;
    }

    private JBrowseChrTrackData getJBrowseChrTrackData(String chr) {
        JBrowseChrTrackData data = null;
        if (!map.containsKey(chr)) {
            try {
                data = JBrowseChrTrackData.create(new File(getTrackDataFilename(chr)));
                map.put(chr, data);
            } catch (Exception e) {
                throw new RuntimeException("Could not load track data");
            }
        } else {
            data = map.get(chr);
        }
        return data;
    }

    private String getTrackDataFilename(String chr) {
        return JBROWSE_DATA_FOLDER + "/tracks/" + chr + "/" + JBROWSE_TRACK_FOLDER + "/trackData.json";
    }

    @PostConstruct
    public void init() {
        SmileFactory factory = new SmileFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        try {
            annotationMap = mapper.readValue(new File(GENE_ANNOTATION_FILE), new TypeReference<Map<String, GeneAnnotation>>() {
            });
        } catch (Exception e) {
        }
    }


}
