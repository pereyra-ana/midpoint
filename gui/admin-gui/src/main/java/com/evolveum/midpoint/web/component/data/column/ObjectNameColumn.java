/**
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.data.column;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class ObjectNameColumn<O extends ObjectType> extends AbstractColumn<SelectableBean<O>, String> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ObjectNameColumn.class);
	
	public ObjectNameColumn(IModel<String> displayModel) {
		super(displayModel, ObjectType.F_NAME.getLocalPart());
	}

	@Override
	public void populateItem(Item<ICellPopulator<SelectableBean<O>>> cellItem, String componentId,
			final IModel<SelectableBean<O>> rowModel) {
		
		IModel<String> labelModel = new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public String getObject() {
				SelectableBean<O> selectableBean = rowModel.getObject();
				O value = selectableBean.getValue();
				if (value == null) {
					OperationResult result = selectableBean.getResult();
					return result.getStatus().toString();
				} else {
					return value.getName().getOrig();
				}
			} 
		};
		
		cellItem.add(new LinkPanel(componentId, labelModel) {
        	private static final long serialVersionUID = 1L;
        	
        	@Override
            public void onClick(AjaxRequestTarget target) {
        		ObjectNameColumn.this.onClick(target, rowModel);
            }

            @Override
            public boolean isEnabled() {
                return ObjectNameColumn.this.isEnabled(rowModel);
            }
		});
	}
	
	public boolean isEnabled(IModel<SelectableBean<O>> rowModel) {
        return true;
    }

    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {
    }

}
