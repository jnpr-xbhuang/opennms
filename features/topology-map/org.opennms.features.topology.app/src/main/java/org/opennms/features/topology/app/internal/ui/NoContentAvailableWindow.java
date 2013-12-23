/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2013 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.features.topology.app.internal.ui;

import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import org.opennms.features.topology.api.GraphContainer;
import org.opennms.features.topology.api.support.VertexHopGraphProvider;
import org.opennms.features.topology.api.topo.Criteria;

public class NoContentAvailableWindow extends Window {

    private final Label noDefaultsAvailable;

    public NoContentAvailableWindow(final GraphContainer graphContainer) {
        super("Node Display Warning");

        setResizable(false);
        setClosable(false);
        setDraggable(true);
        setModal(false);
        setWidth(500, Sizeable.Unit.PIXELS);
        setHeight(300, Sizeable.Unit.PIXELS);

        Label label = new Label("If you do not see any nodes in this view, please try one of the options below" +
                "<ul>" +
                "<li>1. Try adding a node manually via the search box.</li>" +
                "<li>2. Try using the default focus by clicking on the button below.</li>" +
                "<li>3. Try clicking on View->Refresh Now.</li>" +
                "</ul>",  ContentMode.HTML);

        final HorizontalLayout defaultLayout = new HorizontalLayout();
        defaultLayout.setMargin(true);
        defaultLayout.setSpacing(true);
        noDefaultsAvailable = new Label("No nodes found.<br/>Please add nodes manually.", ContentMode.HTML);
        noDefaultsAvailable.setVisible(false);

        Button defaultFocusButton = new Button("Use Default Focus");
        defaultFocusButton.addClickListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                Criteria defaultCriteria = graphContainer.getBaseTopology().getDefaultCriteria();
                if (defaultCriteria != null) {
                    // check if there is already a criteria registered for focus nodes. If so, remove that
                    VertexHopGraphProvider.FocusNodeHopCriteria criteria = VertexHopGraphProvider.getFocusNodeHopCriteriaForContainer(graphContainer, false);
                    if (criteria != null) {
                        graphContainer.removeCriteria(criteria);
                    }
                    graphContainer.addCriteria(defaultCriteria); // add default criteria
                    graphContainer.redoLayout(); // we need to redo the layout
                    noDefaultsAvailable.setVisible(false);
                } else {
                    noDefaultsAvailable.setVisible(true);
                }
            }
        });
        defaultLayout.setMargin(true);
        defaultLayout.addComponent(defaultFocusButton);
        defaultLayout.addComponent(noDefaultsAvailable);

        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setMargin(true);
        contentLayout.addComponent(label);
        contentLayout.addComponent(defaultLayout);

        setContent(contentLayout);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible) {
            noDefaultsAvailable.setVisible(false);
        } else {
            center();
        }
    }
}
