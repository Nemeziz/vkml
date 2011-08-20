package org.vaadin.vkml;

import java.util.Collection;
import java.util.LinkedList;

import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.Form;
import com.vaadin.ui.FormFieldFactory;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Placemark;

public class FeatureForm extends Form implements FormFieldFactory {

	private VerticalLayout layout;
	private DocumentView owner;
	private Feature feature;

	public FeatureForm(final Feature feature, final DocumentView owner) {
		super(new VerticalLayout());
		layout = (VerticalLayout) getLayout();
		setSizeFull();
		layout.setSizeFull();
		this.owner = owner;
		this.feature = feature;
		setFormFieldFactory(this);
		setImmediate(true);
		Item item = new BeanItem<Feature>(feature);
		setItemDataSource(item, getVisibleFieldsForFeature(feature));
		if (feature instanceof Folder) {
			owner.getMap().clear();
			owner.getMap().showFeature(feature);
			owner.getMap().showAllVectors();
		} else if (feature instanceof Placemark) {
			Placemark placemark = (Placemark) feature;
			getLayout().addComponent(
					new GeometryEditor(placemark.getGeometry(), owner));
		}
	}

	@Override
	public void setItemDataSource(Item newDataSource, Collection<?> propertyIds) {
		super.setItemDataSource(newDataSource, propertyIds);
		Field field = getField("description");
		layout.setExpandRatio(field, 1);
		TextField nam = (TextField) getField("name");
		nam.selectAll();
	}

	private Collection<?> getVisibleFieldsForFeature(Feature value) {
		LinkedList<String> visibleProperties = new LinkedList<String>();
		visibleProperties.add("name");
		visibleProperties.add("description");
		if (feature instanceof Placemark) {
			visibleProperties.add("styleUrl");
		}
		return visibleProperties;
	}

	public Field createField(Item item, Object propertyId, Component uiContext) {
		if (propertyId.equals("styleUrl")) {
			NativeSelect nativeSelect = new NativeSelect();
			nativeSelect.setCaption("Style");
			nativeSelect.setContainerDataSource(owner.getStyles());
			return nativeSelect;
		}
		if (propertyId.equals("description")) {
			TextArea textArea = new TextArea();
			textArea.setSizeFull();
			return textArea;
		}
		Field field = DefaultFieldFactory.createFieldByPropertyType(item
				.getItemProperty(propertyId).getType());
		field.setCaption(DefaultFieldFactory
				.createCaptionByPropertyId(propertyId));
		if (propertyId.equals("name")) {
			field.addListener(new ValueChangeListener() {
				public void valueChange(
						com.vaadin.data.Property.ValueChangeEvent event) {
					owner.getTree().setItemCaption(owner.getTree().getValue(),
							feature.getName());
				}
			});
			field.setWidth("100%");
		}
		return field;
	}

}
