package org.vaadin.vkml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.vaadin.vol.Layer;
import org.vaadin.vol.MapTilerLayer;
import org.xml.sax.SAXException;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class BackgroundLayerEditor extends Window {
	
	static List<MapTilerLayer> layers = new ArrayList<MapTilerLayer>();
	
	static {
		try {
			MapTilerLayer l = new MapTilerLayer("http://dl.dropbox.com/u/4041822/pirttikankare/");
			l.setCaption("Example with PR orienteering map");
			l.setDisplayName("Example with PR orienteering map");
			layers.add(l);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private CssLayout layersList;
	
	public BackgroundLayerEditor() {
		setCaption("Layer editor");
		
		addComponent(new Label("Current layers:"));
		
		layersList = new CssLayout();
		for (final Layer l : layers) {
			addLayerToEditor(l);
		}
		
		addComponent(layersList);
		
		addComponent(new Label("Add new:"));
		final TextField url = new TextField("Url");
		final TextField title = new TextField("Title");
		Button button = new Button("Add");
		
		addComponent(url);
		addComponent(title);
		addComponent(button);
		
		button.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				try {
					MapTilerLayer mapTilerLayer = new MapTilerLayer(url.getValue().toString());
					mapTilerLayer.setCaption(title.getValue().toString());
					mapTilerLayer.setDisplayName(mapTilerLayer.getCaption());
					synchronized (layers) {
						layers.add(mapTilerLayer);
					}
					addLayerToEditor(mapTilerLayer);
					showNotification("Layer added successfully");
					return;
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				showNotification("Couldn't add layer.");
			}
		});
		
		
		
		// 
		// TODO Auto-generated constructor stub
	}

	private void addLayerToEditor(final Layer l) {
		final HorizontalLayout horizontalLayout = new HorizontalLayout();
		horizontalLayout.addComponent(new Label(l.getCaption()));
		final Button b = new Button("Delete");
		b.addListener(new ClickListener() {
			public void buttonClick(final ClickEvent event) {
				synchronized (layers) {
					layers.remove(l);
				}
				layersList.removeComponent(horizontalLayout);
			}
		});
		horizontalLayout.addComponent(b);
		layersList.addComponent(horizontalLayout);
	}
	
	public static List<MapTilerLayer> getExtraLayers() {
		return layers;
	}

}
