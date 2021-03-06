package org.aksw.simba.squirrel.analyzer.htmlscraper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.simba.squirrel.analyzer.htmlscraper.exceptions.ElementNotFoundException;
import org.aksw.simba.squirrel.data.uri.UriUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;



/**
 * 
 * @author gsjunior
 *
 */
public class HtmlScraper {
	

	
	private Map<String, YamlFile> yamlFiles = new HashMap<String, YamlFile>();
	
	
	
	public HtmlScraper(File file) {
		try {
			yamlFiles = new YamlFilesParser(file).getYamlFiles();
		} catch (Exception e) {
			
		}
	}
	
	public HtmlScraper() {
		try {
			yamlFiles = new YamlFilesParser().getYamlFiles();
		} catch (Exception e) {
			
		}

	}
	
	public List<Triple> scrape(String uri, File filetToScrape) throws Exception {
		
		List<Triple> listTriples = new ArrayList<Triple>();
		
		YamlFile yamlFile = yamlFiles.get(UriUtils.getDomainName(uri));
		if(yamlFile != null) {
			yamlFile.getSearch().remove(YamlFileAtributes.SEARCH_CHECK);
			
			for(Entry<String,Map<String, Object>> entry : yamlFile.getSearch().entrySet()) {
				for(Entry<String,Object>  cfg : entry.getValue().entrySet()) {
					if(cfg.getKey().equals(YamlFileAtributes.REGEX) && uri.toLowerCase().contains(cfg.getValue().toString().toLowerCase())) {
						@SuppressWarnings("unchecked")
						Map<String, Object> resources = (Map<String, Object>) entry.getValue().get(YamlFileAtributes.RESOURCES);
						listTriples.addAll(scrapeDownloadLink(resources,filetToScrape,uri));
					}
				}
			}
			
		}

		return listTriples;
	}
	
	private Set<Triple> scrapeDownloadLink(Map<String, Object> resources, File htmlFile,String uri) throws Exception {
		Document doc = Jsoup.parse(htmlFile,"UTF-8");
		
		Node s = NodeFactory.createURI(uri);
		
		Set<Triple> listTriples = new LinkedHashSet<Triple>();
		


		List<String> resourcesList = new ArrayList<String>();
		Node objectNode;
		for(Entry<String, Object> entry: 
			resources.entrySet()) {
			resourcesList.clear();
			
			Node p = NodeFactory.createURI(entry.getKey());
			
			if(entry.getValue() instanceof List<?> && ((ArrayList<String>) entry.getValue()).size() > 1) {
				resourcesList = (ArrayList<String>) entry.getValue();
			} else {
				resourcesList.add(entry.getValue().toString());
			}
			
			for(String resource: resourcesList) {
				Elements elements = null;
				
				try {
					elements = doc.select(resource);
					if(elements.isEmpty()) {
						throw new ElementNotFoundException("Element (" + entry.getKey() + " -> "+ resource + ")"
								+ " not found. Check selector syntax");
						
					}
				}catch(Exception e) {
					throw new Exception(e);
				}
				
				for(int i=0; i<elements.size();i++) {
					if(elements.get(i).hasAttr("href")) {
						objectNode = NodeFactory.createURI(elements.get(i).attr("abs:href"));
					}else {
						objectNode = NodeFactory.createLiteral(elements.get(i).text());
					}
					
					Triple triple = new Triple(s, p, objectNode);
					listTriples.add(triple);
				}
	
			}
		}
		
		return listTriples;
	}


}
