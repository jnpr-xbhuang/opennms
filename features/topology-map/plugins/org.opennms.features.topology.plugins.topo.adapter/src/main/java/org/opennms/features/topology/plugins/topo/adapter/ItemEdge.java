package org.opennms.features.topology.plugins.topo.adapter;

import org.opennms.features.topology.api.topo.Edge;

import com.vaadin.data.Item;
import com.vaadin.data.Property;

class ItemEdge implements Edge {
	
	private static final String LABEL = "label";
	private static final String TOOLTIP_TEXT = "tooltipText";
	private static final String STYLE_NAME = "styleName";

	private final String m_namespace;
	private final String m_id;
	private final Object m_itemId;
	private final ItemFinder m_itemFinder;
	private ItemConnector m_source;
	private ItemConnector m_target;

	public ItemEdge(String namespace, String id, Object itemId, ItemFinder itemFinder) {
		m_namespace = namespace;
		m_id = id;
		m_itemId = itemId;
		m_itemFinder = itemFinder;
	}
	
	@Override
	public String getNamespace() {
		return m_namespace;
	}

	@Override
	public String getId() {
		return m_id;
	}
	
	@Override
	public Object getItemId() {
		return m_itemId;
	}

	@Override
	public ItemConnector getSource() {
		return m_source;
	}
	
	public void setSource(ItemConnector source) {
		m_source = source;
	}

	@Override
	public ItemConnector getTarget() {
		return m_target;
	}
	
	public void setTarget(ItemConnector target) {
		m_target = target;
	}

	@Override
    public String getLabel() {
        Property labelProperty = getItem().getItemProperty(LABEL);
		String label = labelProperty == null ? "labels unsupported " : (String) labelProperty.getValue();
		return label;
    }

	private Item getItem() {
		return m_itemFinder.getItem(m_itemId);
	}

	@Override
	public String getTooltipText() {
		if(getItem().getItemProperty(TOOLTIP_TEXT) != null && getItem().getItemProperty(TOOLTIP_TEXT).getValue() != null) {
			return (String) getItem().getItemProperty(TOOLTIP_TEXT).getValue();
		}else {
			return null;
		}
	}

	@Override
	public String getStyleName() {
        Property styleProperty = getItem().getItemProperty(STYLE_NAME);
		return styleProperty == null ? null : (String)styleProperty.getValue();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((m_id == null) ? 0 : m_id.hashCode());
		result = prime * result
				+ ((m_namespace == null) ? 0 : m_namespace.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemVertex other = (ItemVertex) obj;
		if (m_id == null) {
			if (other.m_id != null)
				return false;
		} else if (!m_id.equals(other.m_id))
			return false;
		if (m_namespace == null) {
			if (other.m_namespace != null)
				return false;
		} else if (!m_namespace.equals(other.m_namespace))
			return false;
		return true;
	}

	@Override
	public String getKey() {
		return getNamespace()+":"+getId();
	}

}