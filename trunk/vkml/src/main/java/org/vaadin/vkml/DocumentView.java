package org.vaadin.vkml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import org.vaadin.vol.Style;
import org.vaadin.vol.StyleMap;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.event.ShortcutAction;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.StyleSelector;

public class DocumentView extends CssLayout implements ClickListener,
		Property.ValueChangeListener, Handler {
	private Kml kml;

	CssLayout mainArea = new CssLayout();

	IndexedContainer styles = new IndexedContainer();

	/*
	 * TODO reduce bug if not using hierarchical container
	 */
	private Tree tree = new Tree(null, new HierarchicalContainer());
	public static final Object FEATURE_ROOT = "Features";
	public static final Object FEATURE_PID = new Object();

	private static Action ACTION_NEW = new ShortcutAction("^New");
	private static final Action[] ACTIONS = new Action[] { ACTION_NEW };

	private HorizontalLayout toolbar = new HorizontalLayout();

	private Button save = new NativeButton("", this);
	private Button close = new NativeButton("", this);
	private Button addFolder = new NativeButton("", this);
	private Button addArea = new NativeButton("", this);
	private Button deleteFeature = new NativeButton("", this);

	private Document doc;
	private FeatureMap map;

	private StyleMap styleMap;

	public DocumentView(Kml kml) {
		setSizeFull();
		HorizontalSplitPanel horizontalSplitPanel = new HorizontalSplitPanel();
		addComponent(horizontalSplitPanel);
		horizontalSplitPanel.setSplitPosition(320, UNITS_PIXELS);
		VerticalLayout vl = new VerticalLayout();
		populateToolbar();
		vl.addComponent(toolbar);
		vl.setSizeFull();
		VerticalSplitPanel verticalSplitPanel = new VerticalSplitPanel();
		verticalSplitPanel.addComponent(tree);
		verticalSplitPanel.addComponent(mainArea);

		vl.addComponent(verticalSplitPanel);
		vl.setExpandRatio(verticalSplitPanel, 1);

		mainArea.setSizeFull();
		mainArea.setMargin(true);

		horizontalSplitPanel.addComponent(vl);
		horizontalSplitPanel.addComponent(getMap());

		tree.addListener(this);
		tree.setImmediate(true);
		tree.setNullSelectionAllowed(false);
		tree.setSelectable(true);
		tree.addContainerProperty(FEATURE_PID, Feature.class, null);

		this.kml = kml;
		init();
	}

	@Override
	public void attach() {
		super.attach();
		getWindow().addActionHandler(this);
	}

	@Override
	public void detach() {
		getWindow().removeActionHandler(this);
		super.detach();
	}

	public FeatureMap getMap() {
		if (map == null) {
			map = new FeatureMap(this);
		}
		return map;
	}

	private void populateToolbar() {
		toolbar.addComponent(close);
		toolbar.addComponent(save);
		toolbar.addComponent(addFolder);
		toolbar.addComponent(addArea);
		toolbar.addComponent(deleteFeature);
		save.setDescription("Save (download) the KML file.");
		save.setIcon(new ThemeResource("../vgis/document-save.png"));
		close.setIcon(new ThemeResource("../vgis/go-home.png"));
		close.setDescription("Close current document and return to start screen.");
		addFolder.setIcon(new ThemeResource("../vgis/folder-new.png"));
		addFolder.setDescription("Add new folder to the document");
		addArea.setIcon(new ThemeResource("../vgis/document-new.png"));
		addArea.setDescription("Add new area to KML file (^N)");

		deleteFeature.setIcon(new ThemeResource("../vgis/edit-clear.png"));
		deleteFeature.setDescription("Remove selected item.");

		OptionGroup optionGroup = new OptionGroup();
		optionGroup.addItem("Pan");
		optionGroup.addItem("Modify");
		optionGroup.setValue("Pan");
		optionGroup.setImmediate(true);
		toolbar.addComponent(optionGroup);
		optionGroup.addListener(new Property.ValueChangeListener() {
			public void valueChange(ValueChangeEvent event) {
				getMap().setModifiable(event.getProperty().getValue().equals("Modify"));
			}
		});

	}

	private void init() {
		tree.addItem(FEATURE_ROOT);
		tree.setItemCaption(FEATURE_ROOT, kml.getFeature().getName());
		tree.expandItem(FEATURE_ROOT);

		Feature feature = kml.getFeature();
		if (feature instanceof Document) {
			doc = (Document) feature;
			List<Feature> features = doc.getFeature();
			for (Feature f : features) {
				if (f instanceof Folder) {
					Folder folder = (Folder) f;
					addFolder(folder, FEATURE_ROOT);
				} else if (f instanceof Placemark) {
					Placemark pm = (Placemark) f;
					addFeature(pm, FEATURE_ROOT);
				}
			}

			List<StyleSelector> styleSelectors = doc.getStyleSelector();

			styleMap = new StyleMap();
			for (StyleSelector styleSelector2 : styleSelectors) {
				de.micromata.opengis.kml.v_2_2_0.Style s = (de.micromata.opengis.kml.v_2_2_0.Style) styleSelector2;
				styles.addItem(styleSelector2.getId());

				Style style = new Style();
				styleMap.setStyle(styleSelector2.getId(), style);

				String substring = s.getLineStyle().getColor().substring(0, 6);
				style.setStrokeColor("#" + s);
				substring = s.getPolyStyle().getColor().substring(0, 6);
				style.setFillColor(substring);

			}
			getMap().setStyleMap(styleMap);
		}
		tree.setValue(FEATURE_ROOT);
	}

	private Object addFeature(Feature pm, Object parentItemId) {
		Object itemId = tree.addItem();
		tree.getItem(itemId).getItemProperty(FEATURE_PID).setValue(pm);
		tree.setItemCaption(itemId, pm.getName());
		if (parentItemId != null) {
			tree.setParent(itemId, parentItemId);
		}
		tree.setChildrenAllowed(itemId, pm instanceof Folder);
		return itemId;
	}

	private Object addFolder(Folder folder, Object parentItemId) {
		Object itemId = addFeature(folder, parentItemId);
		if (folder.isOpen()) {
			tree.expandItem(itemId);
		}

		for (Feature f : folder.getFeature()) {
			if (f instanceof Folder) {
				addFolder((Folder) f, itemId);
			} else if (f instanceof Placemark) {
				Placemark pm = (Placemark) f;
				Object pmId = addFeature(pm, itemId);
				tree.setChildrenAllowed(pmId, false);
			} else {
				System.err.println(f);
			}
		}
		return itemId;
	}

	public void buttonClick(ClickEvent event) {
		if (event.getButton() == save) {
			saveKml();
		} else if (event.getButton() == close) {
			closeDocument();
		} else if (event.getButton() == addFolder) {
			addFolder();
		} else if (event.getButton() == addArea) {
			addArea();
		} else if (event.getButton() == deleteFeature) {
			deleteSelectedFeature();
		} else {
			System.err.println("Unhandled click");
		}
	}

	private void closeDocument() {
		getApplication().close();
	}

	private void deleteSelectedFeature() {
		Object value = tree.getValue();
		if (value == null || value == FEATURE_ROOT) {
			getWindow().showNotification("Select a feature to delete");
			return;
		}

		Feature feature = (Feature) tree.getItem(value)
				.getItemProperty(FEATURE_PID).getValue();
		Object parentId = tree.getParent(value);
		List<Feature> featureList;
		if (parentId == FEATURE_ROOT) {
			featureList = doc.getFeature();
		} else {
			Folder parent = (Folder) tree.getItem(parentId)
					.getItemProperty(FEATURE_PID).getValue();
			featureList = parent.getFeature();
		}
		featureList.remove(feature);
		tree.removeItem(value);
		tree.select(parentId);
	}

	private void addArea() {
		Object parentFolderId = getParentFolderId();
		List<Feature> featureListWhereToAdd = getFeatureListById(parentFolderId);
		Placemark newArea = createEmptyArea();
		featureListWhereToAdd.add(newArea);
		Object newItemId = addFeature(newArea, parentFolderId);
		tree.setValue(newItemId);
	}

	private Placemark createEmptyArea() {
		Placemark placemark = new Placemark();
		placemark.setName("new area");
		placemark.setDescription("");
		placemark.setStyleUrl((String) styles.firstItemId());
		Polygon polygon = new Polygon();
		polygon.setAltitudeMode(AltitudeMode.RELATIVE_TO_GROUND);
		Boundary boundary = new Boundary();
		LinearRing linearRing = new LinearRing();
		placemark.setGeometry(polygon);
		polygon.setOuterBoundaryIs(boundary);
		boundary.setLinearRing(linearRing);
		polygon.setTessellate(true);
		return placemark;
	}

	private List<Feature> getFeatureListById(Object parentFolderId) {
		if (parentFolderId == FEATURE_ROOT) {
			return doc.getFeature();
		} else {
			Folder folder = (Folder) tree.getItem(parentFolderId)
					.getItemProperty(FEATURE_PID).getValue();
			return folder.getFeature();
		}
	}

	private Object getParentFolderId() {
		Object id = tree.getValue();
		if (id == null || id == FEATURE_ROOT) {
			return FEATURE_ROOT;
		}

		Item item = tree.getItem(id);
		if (item.getItemProperty(FEATURE_PID) != null
				&& item.getItemProperty(FEATURE_PID).getValue() instanceof Folder) {
			return id;
		} else {
			return tree.getParent(id);
		}
	}

	private void addFolder() {
		Folder newFolder = new Folder();
		newFolder.getFeature(); // side effect to create
		newFolder.setName("New folder");
		newFolder.setOpen(true);

		Object parentId = getParentFolderId();
		List<Feature> parent = null;
		Object addedId = null;
		if (parentId != null) {
			if (parentId == FEATURE_ROOT) {
				parent = doc.getFeature();
			}
			try {
				parent = ((Folder) tree.getItem(parentId)
						.getItemProperty(FEATURE_PID).getValue()).getFeature();
			} catch (Exception e) {

				// TODO: handle exception
			}
		}
		if (parent == null) {
			getWindow().showNotification("Select a parent folder");
			return;
		} else {
			addedId = addFolder(newFolder, parentId);
			parent.add(newFolder);
		}
		tree.setValue(addedId);
	}

	private void saveKml() {
		com.vaadin.terminal.StreamResource.StreamSource ss = new com.vaadin.terminal.StreamResource.StreamSource() {

			public InputStream getStream() {
				try {
					ByteArrayOutputStream outputstream = new ByteArrayOutputStream();
					kml.marshal(outputstream);
					return new ByteArrayInputStream(outputstream.toByteArray());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				return null;
			}
		};

		StreamResource streamResource = new StreamResource(ss, kml.getFeature()
				.getName() + ".kml", getApplication());
		streamResource.setMIMEType("application/vnd.google-earth.kml+xml");
		streamResource.setCacheTime(0);

		getWindow().open(streamResource);
	}

	public void valueChange(ValueChangeEvent event) {
		if (event.getProperty() == tree && tree.getValue() != null) {
			Item item = tree.getItem(tree.getValue());
			Property itemProperty = item.getItemProperty(FEATURE_PID);
			if (itemProperty != null && itemProperty.getValue() != null) {
				Feature feature = (Feature) itemProperty.getValue();
				editFeature(feature);
			} else if (tree.getValue() == FEATURE_ROOT) {
				getMap().clear();
				getMap().showFeature(doc);
				getMap().showAllVectors();
			}
		}
	}

	private void editFeature(final Feature value) {
		mainArea.removeAllComponents();
		mainArea.addComponent(new FeatureForm(value, this));
	}

	public Tree getTree() {
		return tree;
	}

	public Container getStyles() {
		return styles;
	}

	public Action[] getActions(Object target, Object sender) {
		return ACTIONS;
	}

	public void handleAction(Action action, Object sender, Object target) {
		if (action == ACTION_NEW) {
			addArea();
		} else {
			throw new UnsupportedOperationException();
		}
	}

	public void selectFeatureByGeometry(Geometry data) {
		for (Object itemId : tree.getItemIds()) {
			Object feature = tree.getItem(itemId).getItemProperty(FEATURE_PID)
					.getValue();
			if (feature instanceof Placemark) {
				Placemark pm = (Placemark) feature;
				if (pm.getGeometry() == data) {
					tree.setValue(itemId);
					return;
				}
			}
		}
	}

	public StyleMap getStyleMap() {
		return styleMap;
	}
}
