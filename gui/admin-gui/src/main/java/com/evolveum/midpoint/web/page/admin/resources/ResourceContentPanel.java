/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.FocusBrowserPanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ColumnTypeDto;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.resources.ResourceContentTabPanel.Operation;
import com.evolveum.midpoint.web.page.admin.resources.content.PageAccount;
import com.evolveum.midpoint.web.page.admin.server.PageTaskAdd;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Implementation classes : ResourceContentResourcePanel,
 * ResourceContentRepositoryPanel
 * 
 * @author katkav
 * @author semancik
 */
public abstract class ResourceContentPanel extends Panel {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ResourceContentPanel.class);

	private static final String DOT_CLASS = ResourceContentTabPanel.class.getName() + ".";
	private static final String OPERATION_SEARCH_TASKS_FOR_RESOURCE = DOT_CLASS + "seachTasks";
	private static final String OPERATION_CHANGE_OWNER = DOT_CLASS + "changeOwner";
	private static final String OPERATION_LOAD_SHADOW_OWNER = DOT_CLASS + "loadOwner";
	private static final String OPERATION_UPDATE_STATUS = DOT_CLASS + "updateStatus";
	private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
	private static final String OPERATION_IMPORT_OBJECT = DOT_CLASS + "importObject";

	private static final String ID_TABLE = "table";
	private static final String ID_LABEL = "label";

	private static final String ID_IMPORT = "import";
	private static final String ID_RECONCILIATION = "reconciliation";
	private static final String ID_LIVE_SYNC = "liveSync";

	private PageBase pageBase;
	private ShadowKindType kind;
	private String intent;
	private QName objectClass;

	IModel<PrismObject<ResourceType>> resourceModel;

	public ResourceContentPanel(String id, IModel<PrismObject<ResourceType>> resourceModel, QName objectClass,
			ShadowKindType kind, String intent, PageBase pageBase) {
		super(id);
		this.pageBase = pageBase;
		this.kind = kind;
		this.resourceModel = resourceModel;
		this.intent = intent;
		this.objectClass = objectClass;
		initLayout();
	}

	public PageBase getPageBase() {
		return pageBase;
	}

	public ShadowKindType getKind() {
		return kind;
	}

	public String getIntent() {
		return intent;
	}

	public IModel<PrismObject<ResourceType>> getResourceModel() {
		return resourceModel;
	}

	public QName getObjectClass() {
		return objectClass;
	}

	public RefinedObjectClassDefinition getDefinitionByKind() throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema
				.getRefinedSchema(resourceModel.getObject(), getPageBase().getPrismContext());
		return refinedSchema.getRefinedDefinition(getKind(), getIntent());

	}

	public RefinedObjectClassDefinition getDefinitionByObjectClass() throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema
				.getRefinedSchema(resourceModel.getObject(), getPageBase().getPrismContext());
		return refinedSchema.getRefinedDefinition(getObjectClass());

	}

	private void initLayout() {

		MainObjectListPanel<ShadowType> shadowListPanel = new MainObjectListPanel<ShadowType>(ID_TABLE,
				ShadowType.class, TableId.PAGE_RESOURCE_ACCOUNTS_PANEL, null, pageBase) {
			private static final long serialVersionUID = 1L;

			@Override
			protected List<InlineMenuItem> createInlineMenu() {
				return ResourceContentPanel.this.createRowMenuItems();
			}

			@Override
			protected List<IColumn<SelectableBean<ShadowType>, String>> createColumns() {
				return ResourceContentPanel.this.initColumns();
			}

			@Override
			protected void objectDetailsPerformed(AjaxRequestTarget target, ShadowType object) {
				shadowDetailsPerformed(target, WebComponentUtil.getName(object), object.getOid());

			}

			@Override
			protected void newObjectPerformed(AjaxRequestTarget target) {
				// TODO Auto-generated method stub

			}

			@Override
			protected BaseSortableDataProvider<SelectableBean<ShadowType>> initProvider() {
				SelectableBeanObjectDataProvider<ShadowType> provider = (SelectableBeanObjectDataProvider<ShadowType>) super.initProvider();
				provider.setEmptyListOnNullQuery(true);
				provider.setSort(null);
				createSearchOptions(provider);
				return provider;
			}

			@Override
			protected ObjectQuery createContentQuery() {
				ObjectQuery parentQuery = super.createContentQuery();

				List<ObjectFilter> filters = new ArrayList<>();
				if (parentQuery != null) {
					filters.add(parentQuery.getFilter());
				}

				ObjectQuery customQuery = ResourceContentPanel.this.createQuery();
				if (customQuery != null && customQuery.getFilter() != null) {
					filters.add(customQuery.getFilter());
				}

				if (filters.size() == 1) {
					return ObjectQuery.createObjectQuery(filters.iterator().next());
				}

				if (filters.size() == 0) {
					return null;
				}

				return ObjectQuery.createObjectQuery(AndFilter.createAnd(filters));
			}

			@Override
			protected LoadableModel<Search> createSearchModel() {
				return new LoadableModel<Search>(false) {
					private static final long serialVersionUID = 1L;

					@Override
					public Search load() {

						return ResourceContentPanel.this.createSearch();
					}
				};
			}
		};
		shadowListPanel.setOutputMarkupId(true);
		shadowListPanel.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return createQuery() != null;
			}
		});
		shadowListPanel.setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_SHADOW_BOX_CSS_CLASSES);
		add(shadowListPanel);

		Label label = new Label(ID_LABEL, "Nothing to show. Select intent to search");
		add(label);
		label.setOutputMarkupId(true);
		label.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return createQuery() == null;
			}
		});

		OperationResult result = new OperationResult(OPERATION_SEARCH_TASKS_FOR_RESOURCE);

		List<PrismObject<TaskType>> tasks = WebModelServiceUtils
				.searchObjects(TaskType.class,
						ObjectQuery.createObjectQuery(RefFilter.createReferenceEqual(TaskType.F_OBJECT_REF,
								TaskType.class, getPageBase().getPrismContext(),
								getResourceModel().getObject().getOid())),
						result, getPageBase());

		List<TaskType> tasksForKind = getTasksForKind(tasks);

		List<TaskType> importTasks = new ArrayList<>();
		List<TaskType> syncTasks = new ArrayList<>();
		List<TaskType> reconTasks = new ArrayList<>();
		for (TaskType task : tasksForKind) {
			if (TaskCategory.RECONCILIATION.equals(task.getCategory())) {
				reconTasks.add(task);
			} else if (TaskCategory.LIVE_SYNCHRONIZATION.equals(task.getCategory())) {
				syncTasks.add(task);
			} else if (TaskCategory.IMPORTING_ACCOUNTS.equals(task.getCategory())) {
				importTasks.add(task);
			}
		}

		initButton(ID_IMPORT, "Import", " fa-download", TaskCategory.IMPORTING_ACCOUNTS, importTasks);
		initButton(ID_RECONCILIATION, "Reconciliation", " fa-link", TaskCategory.RECONCILIATION, reconTasks);
		initButton(ID_LIVE_SYNC, "Live Sync", " fa-refresh", TaskCategory.LIVE_SYNCHRONIZATION, syncTasks);

		initCustomLayout();
	}

	private void initButton(String id, String label, String icon, final String category,
			final List<TaskType> tasks) {

		List<InlineMenuItem> items = new ArrayList<>();

		InlineMenuItem item = new InlineMenuItem(
				getPageBase().createStringResource("ResourceContentResourcePanel.showExisting"),
				new InlineMenuItemAction() {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						runTask(tasks, target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(getPageBase().createStringResource("ResourceContentResourcePanel.newTask"),
				new InlineMenuItemAction() {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						newTaskPerformed(category, target);
					}
				});
		items.add(item);

		DropdownButtonPanel button = new DropdownButtonPanel(id,
				new DropdownButtonDto(String.valueOf(tasks.size()), icon, label, items));
		add(button);

	}

	private void newTaskPerformed(String category, AjaxRequestTarget target) {
		TaskType taskType = new TaskType();
		PrismProperty<ShadowKindType> pKind;
		try {
			pKind = taskType.asPrismObject().findOrCreateProperty(
					new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND));
			pKind.setRealValue(getKind());
		} catch (SchemaException e) {
			getSession().warn("Could not set kind for new task " + e.getMessage());
		}

		PrismProperty<String> pIntent;
		try {
			pIntent = taskType.asPrismObject().findOrCreateProperty(
					new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));
			pIntent.setRealValue(getIntent());
		} catch (SchemaException e) {
			getSession().warn("Could not set kind for new task " + e.getMessage());
		}

		PrismObject<ResourceType> resource = getResourceModel().getObject();
		taskType.setObjectRef(ObjectTypeUtil.createObjectRef(resource));

		taskType.setCategory(category);
		setResponsePage(new PageTaskAdd(taskType));
		;
	}

	private void runTask(List<TaskType> tasks, AjaxRequestTarget target) {

		ResourceTasksPanel tasksPanel = new ResourceTasksPanel(getPageBase().getMainPopupBodyId(), false,
				new ListModel<>(tasks), getPageBase());
		getPageBase().showMainPopup(tasksPanel, target);

	}

	private List<TaskType> getTasksForKind(List<PrismObject<TaskType>> tasks) {
		List<TaskType> tasksForKind = new ArrayList<>();
		for (PrismObject<TaskType> task : tasks) {
			PrismProperty<ShadowKindType> taskKind = task
					.findProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND));
			ShadowKindType taskKindValue = null;
			if (taskKind != null) {
				taskKindValue = taskKind.getRealValue();

				PrismProperty<String> taskIntent = task.findProperty(
						new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));
				String taskIntentValue = null;
				if (taskIntent != null) {
					taskIntentValue = taskIntent.getRealValue();
				}
				if (StringUtils.isNotEmpty(getIntent())) {
					if (getKind() == taskKindValue && getIntent().equals(taskIntentValue)) {
						tasksForKind.add(task.asObjectable());
					}
				} else if (getKind() == taskKindValue) {
					tasksForKind.add(task.asObjectable());
				}
			}
		}
		return tasksForKind;
	}

	// private SelectableBeanObjectDataProvider<ShadowType> initProvider() {
	// SelectableBeanObjectDataProvider<ShadowType> provider = new
	// SelectableBeanObjectDataProvider<ShadowType>(
	// this, ShadowType.class) {
	// private static final long serialVersionUID = 1L;
	//
	// @Override
	// public SelectableBean<ShadowType> createDataObjectWrapper(ShadowType obj)
	// {
	// SelectableBean<ShadowType> bean = super.createDataObjectWrapper(obj);
	// List<InlineMenuItem> inlineMenu = createRowMenuItems();
	// if (inlineMenu != null) {
	// bean.getMenuItems().addAll(inlineMenu);
	// }
	// return bean;
	// }
	// };
	//
	// ObjectQuery query = createQuery();
	//
	// if (query == null) {
	// initCustomLayout();
	// return provider;
	//
	// }
	//
	// provider.setEmptyListOnNullQuery(true);
	// provider.setSort(null);
	// createSearchOptions(provider);
	// return provider;
	// }

	protected void initCustomLayout() {
		// Nothing to do, for subclass extension
	};

	protected ObjectQuery createQuery() {
		ObjectQuery baseQuery = null;

		try {
			if (kind == null) {
				if (objectClass == null) {
					return null;
				}
				return ObjectQueryUtil.createResourceAndObjectClassQuery(resourceModel.getObject().getOid(),
						objectClass, getPageBase().getPrismContext());
			}

			RefinedObjectClassDefinition rOcDef = getDefinitionByKind();
			if (rOcDef != null) {
				if (rOcDef.getKind() != null) {
					baseQuery = ObjectQueryUtil.createResourceAndKindIntent(
							resourceModel.getObject().getOid(), rOcDef.getKind(), rOcDef.getIntent(),
							getPageBase().getPrismContext());
				} else {
					baseQuery = ObjectQueryUtil.createResourceAndObjectClassQuery(
							resourceModel.getObject().getOid(), rOcDef.getTypeName(),
							getPageBase().getPrismContext());
				}
			}
		} catch (SchemaException ex) {
			LoggingUtils.logException(LOGGER, "Could not crate query for shadows: " + ex.getMessage(), ex);
		}
		return baseQuery;
	}

	protected abstract Search createSearch();

	private void createSearchOptions(SelectableBeanObjectDataProvider<ShadowType> provider) {

		Collection<SelectorOptions<GetOperationOptions>> opts = SelectorOptions.createCollection(
				ShadowType.F_ASSOCIATION, GetOperationOptions.createRetrieve(RetrieveOption.EXCLUDE));

		if (addAdditionalOptions() != null) {
			opts.add(addAdditionalOptions());
		}
		provider.setUseObjectCounting(isUseObjectCounting());
		provider.setOptions(opts);
	}

	private StringResourceModel createStringResource(String key) {
		return pageBase.createStringResource(key);
	}

	private List<IColumn<SelectableBean<ShadowType>, String>> initColumns() {

		List<ColumnTypeDto<String>> columnDefs = Arrays.asList(
				new ColumnTypeDto<String>("ShadowType.synchronizationSituation",
						SelectableBean.F_VALUE + ".synchronizationSituation",
						ShadowType.F_SYNCHRONIZATION_SITUATION.getLocalPart()),
				new ColumnTypeDto<String>("ShadowType.intent", SelectableBean.F_VALUE + ".intent",
						ShadowType.F_INTENT.getLocalPart()));

		List<IColumn<SelectableBean<ShadowType>, String>> columns = new ArrayList<>();

		IColumn<SelectableBean<ShadowType>, String> column = new AbstractColumn<SelectableBean<ShadowType>, String>(
				createStringResource("pageContentAccounts.identifiers")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<ShadowType>>> cellItem,
					String componentId, IModel<SelectableBean<ShadowType>> rowModel) {

				SelectableBean<ShadowType> dto = rowModel.getObject();
				RepeatingView repeater = new RepeatingView(componentId);

				ShadowType value = dto.getValue();
				if (value != null) {
					for (ResourceAttribute<?> attr : ShadowUtil.getAllIdentifiers(value)) {
						repeater.add(new Label(repeater.newChildId(),
								attr.getElementName().getLocalPart() + ": " + attr.getRealValue()));

					}
				}
				cellItem.add(repeater);

			}
		};
		columns.add(column);

		columns.addAll((Collection) ColumnUtils.createColumns(columnDefs));
		column = new LinkColumn<SelectableBean<ShadowType>>(createStringResource("pageContentAccounts.owner"),
				true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<FocusType> createLinkModel(final IModel<SelectableBean<ShadowType>> rowModel) {

				return new AbstractReadOnlyModel<FocusType>() {
					private static final long serialVersionUID = 1L;

					@Override
					public FocusType getObject() {
						FocusType owner = loadShadowOwner(rowModel);
						if (owner == null) {
							return null;
						}
						return owner;

					}

				};
			}

			@Override
			public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ShadowType>> rowModel) {
				ownerDetailsPerformed(target, this.getModelObjectIdentifier());
			}
		};
		columns.add(column);

		columns.add(new LinkColumn<SelectableBean<ShadowType>>(
				createStringResource("PageAccounts.accounts.result")) {
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createLinkModel(final IModel<SelectableBean<ShadowType>> rowModel) {
				return new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return getResultLabel(rowModel);
					}
				};
			}

			@Override
			public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ShadowType>> rowModel) {
				OperationResultType resultType = getResult(rowModel);
				OperationResult result = OperationResult.createOperationResult(resultType);

				OperationResultPanel body = new OperationResultPanel(
						ResourceContentPanel.this.getPageBase().getMainPopupBodyId(),
						new Model<OpResult>(OpResult.getOpResult(pageBase, result)));
				body.setOutputMarkupId(true);
				ResourceContentPanel.this.getPageBase().showMainPopup(body,
						target);

			}
		});

		column = new InlineMenuHeaderColumn(createHeaderMenuItems());
		columns.add(column);

		return columns;
	}

	private OperationResultType getResult(IModel<SelectableBean<ShadowType>> model) {
		ShadowType shadow = getShadow(model);
		if (shadow == null) {
			return null;
		}
		OperationResultType result = shadow.getResult();
		if (result == null) {
			return null;
		}
		return result;
	}

	private String getResultLabel(IModel<SelectableBean<ShadowType>> model) {

		OperationResultType result = getResult(model);
		if (result == null) {
			return "";
		}

		StringBuilder b = new StringBuilder(
				createStringResource("FailedOperationTypeType." + getShadow(model).getFailedOperationType())
						.getObject());
		b.append(":");
		b.append(createStringResource("OperationResultStatusType." + result.getStatus()).getObject());

		return b.toString();
	}

	private ShadowType getShadow(IModel<SelectableBean<ShadowType>> model) {
		if (model == null || model.getObject() == null || model.getObject().getValue() == null) {
			return null;
		}

		return (ShadowType) model.getObject().getValue();
	}

	private void ownerDetailsPerformed(AjaxRequestTarget target, String ownerOid) {
		if (StringUtils.isEmpty(ownerOid)) {

			return;
		}

		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, ownerOid);
		setResponsePage(PageUser.class, parameters);
	}

	private <F extends FocusType> F loadShadowOwner(IModel<SelectableBean<ShadowType>> model) {
		ShadowType shadow = model.getObject().getValue();
		String shadowOid;
		if (shadow != null) {
			shadowOid = shadow.getOid();
		} else {
			return null;
		}

		return loadShadowOwner(shadowOid);

		// Task task = pageBase.createSimpleTask(OPERATION_LOAD_SHADOW_OWNER);
		// OperationResult result = new
		// OperationResult(OPERATION_LOAD_SHADOW_OWNER);
		//
		// try {
		// PrismObject<? extends FocusType> prismOwner =
		// pageBase.getModelService().searchShadowOwner(shadowOid, null, task,
		// result);
		//
		// if (prismOwner != null) {
		// owner = (F) prismOwner.asObjectable();
		// }
		// } catch (ObjectNotFoundException exception) {
		// // owner was not found, it's possible and it's ok on unlinked
		// // accounts
		// } catch (Exception ex) {
		// result.recordFatalError(pageBase.getString("PageAccounts.message.ownerNotFound",
		// shadowOid), ex);
		// LoggingUtils.logException(LOGGER, "Could not load owner of account
		// with oid: " + shadowOid, ex);
		// } finally {
		// result.computeStatusIfUnknown();
		// }
		//
		// if (WebComponentUtil.showResultInPage(result)) {
		// pageBase.showResult(result, false);
		// }
		//
		// return owner;
	}

	private void shadowDetailsPerformed(AjaxRequestTarget target, String accountName, String accountOid) {
		if (StringUtils.isEmpty(accountOid)) {
			error(pageBase.getString("pageContentAccounts.message.cantShowAccountDetails", accountName,
					accountOid));
			target.add(pageBase.getFeedbackPanel());
			return;
		}

		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, accountOid);
		setResponsePage(PageAccount.class, parameters);
	}

	private <F extends FocusType> F loadShadowOwner(String shadowOid) {

		Task task = pageBase.createSimpleTask(OPERATION_LOAD_SHADOW_OWNER);
		OperationResult result = new OperationResult(OPERATION_LOAD_SHADOW_OWNER);

		try {
			PrismObject<? extends FocusType> prismOwner = pageBase.getModelService()
					.searchShadowOwner(shadowOid, null, task, result);

			if (prismOwner != null) {
				return (F) prismOwner.asObjectable();
			}
		} catch (ObjectNotFoundException exception) {
			// owner was not found, it's possible and it's ok on unlinked
			// accounts
		} catch (Exception ex) {
			result.recordFatalError(pageBase.getString("PageAccounts.message.ownerNotFound", shadowOid), ex);
			LoggingUtils.logException(LOGGER, "Could not load owner of account with oid: " + shadowOid, ex);
		} finally {
			result.computeStatusIfUnknown();
		}

		if (WebComponentUtil.showResultInPage(result)) {
			pageBase.showResult(result, false);
		}

		return null;
	}

	private List<InlineMenuItem> createHeaderMenuItems() {
		List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.enableAccounts"), true,
				new HeaderMenuAction(this) {
					private static final long serialVersionUID = 1L;

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						updateResourceObjectStatusPerformed(null, target, true);
					}
				}));

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.disableAccounts"), true,
				new HeaderMenuAction(this) {
					private static final long serialVersionUID = 1L;

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						updateResourceObjectStatusPerformed(null, target, false);
					}
				}));

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.deleteAccounts"), true,
				new HeaderMenuAction(this) {
					private static final long serialVersionUID = 1L;

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						deleteResourceObjectPerformed(null, target);
					}
				}));

		items.add(new InlineMenuItem());

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.importAccounts"), true,
				new HeaderMenuAction(this) {

					private static final long serialVersionUID = 1L;

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						importResourceObject(null, target);
					}
				}));

		items.add(new InlineMenuItem());

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.removeOwners"), true,
				new HeaderMenuAction(this) {
					private static final long serialVersionUID = 1L;

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						changeOwner(null, target, null, Operation.REMOVE);
					}
				}));

		return items;
	}

	@SuppressWarnings("serial")
	private List<InlineMenuItem> createRowMenuItems() {
		List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.enableAccount"), true,
				new ColumnMenuAction<SelectableBean<ShadowType>>() {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						SelectableBean<ShadowType> shadow = getRowModel().getObject();
						updateResourceObjectStatusPerformed(shadow.getValue(), target, true);
					}
				}));

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.disableAccount"), true,
				new ColumnMenuAction<SelectableBean<ShadowType>>() {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						SelectableBean<ShadowType> shadow = getRowModel().getObject();
						updateResourceObjectStatusPerformed(shadow.getValue(), target, false);
					}
				}));

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.deleteAccount"), true,
				new ColumnMenuAction<SelectableBean<ShadowType>>() {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						SelectableBean<ShadowType> shadow = getRowModel().getObject();
						deleteResourceObjectPerformed(shadow.getValue(), target);
					}
				}));

		items.add(new InlineMenuItem());

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.importAccount"), true,
				new ColumnMenuAction<SelectableBean<ShadowType>>() {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						SelectableBean<ShadowType> shadow = getRowModel().getObject();
						importResourceObject(shadow.getValue(), target);
					}
				}));

		items.add(new InlineMenuItem());

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.removeOwner"), true,
				new ColumnMenuAction<SelectableBean<ShadowType>>() {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						SelectableBean<ShadowType> shadow = getRowModel().getObject();
						changeOwner(shadow.getValue(), target, null, Operation.REMOVE);
					}
				}));

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.changeOwner"), true,
				new ColumnMenuAction<SelectableBean<ShadowType>>() {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						final SelectableBean<ShadowType> shadow = getRowModel().getObject();
						FocusBrowserPanel<UserType> browser = new FocusBrowserPanel<UserType>(
								pageBase.getMainPopupBodyId(), UserType.class,
								WebComponentUtil.createFocusTypeList(), false, pageBase) {

							@Override
							protected void onSelectPerformed(AjaxRequestTarget target, UserType focus) {
								changeOwner(shadow.getValue(), target, focus, Operation.MODIFY);
							}

						};

						pageBase.showMainPopup(browser, target);

					}
				}));

		return items;
	}

	protected void importResourceObject(ShadowType selected, AjaxRequestTarget target) {
		List<ShadowType> selectedShadow = null;
		if (selected != null) {
			selectedShadow = new ArrayList<>();
			selectedShadow.add(selected);
		} else {
			selectedShadow = getTable().getSelectedObjects();
		}

		OperationResult result = new OperationResult(OPERATION_IMPORT_OBJECT);
		Task task = pageBase.createSimpleTask(OPERATION_IMPORT_OBJECT);

		if (selectedShadow == null || selectedShadow.isEmpty()) {
			result.recordWarning("Nothing select to import");
			getPageBase().showResult(result);
			target.add(getPageBase().getFeedbackPanel());
			return;
		}

		for (ShadowType shadow : selectedShadow) {
			try {
				getPageBase().getModelService().importFromResource(shadow.getOid(), task, result);
			} catch (ObjectNotFoundException | SchemaException | SecurityViolationException
					| CommunicationException | ConfigurationException e) {
				result.recordPartialError("Could not import account " + shadow, e);
				LOGGER.error("Could not import account {} ", shadow, e);
				continue;
			}
		}

		result.computeStatusIfUnknown();
		getPageBase().showResult(result);
		getTable().refreshTable(null, target);
		target.add(getPageBase().getFeedbackPanel());
	}

	// TODO: as a task?
	protected void deleteResourceObjectPerformed(ShadowType selected, AjaxRequestTarget target) {
		List<ShadowType> selectedShadow = null;
		if (selected != null) {
			selectedShadow = new ArrayList<>();
			selectedShadow.add(selected);
		} else {
			selectedShadow = getTable().getSelectedObjects();
		}

		OperationResult result = new OperationResult(OPERATION_DELETE_OBJECT);
		Task task = pageBase.createSimpleTask(OPERATION_DELETE_OBJECT);

		if (selectedShadow == null || selectedShadow.isEmpty()) {
			result.recordWarning("Nothing selected to delete");
			getPageBase().showResult(result);
			target.add(getPageBase().getFeedbackPanel());
			return;
		}

		ModelExecuteOptions opts = createModelOptions();

		for (ShadowType shadow : selectedShadow) {
			try {
				ObjectDelta<ShadowType> deleteDelta = ObjectDelta.createDeleteDelta(ShadowType.class,
						shadow.getOid(), getPageBase().getPrismContext());
				getPageBase().getModelService().executeChanges(
						WebComponentUtil.createDeltaCollection(deleteDelta), opts, task, result);
			} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
					| ExpressionEvaluationException | CommunicationException | ConfigurationException
					| PolicyViolationException | SecurityViolationException e) {
				result.recordPartialError("Could not delete object " + shadow, e);
				LOGGER.error("Could not delete {}, using option {}", shadow, opts, e);
				continue;
			}
		}

		result.computeStatusIfUnknown();
		getPageBase().showResult(result);
		getTable().refreshTable(null, target);
		target.add(getPageBase().getFeedbackPanel());

	}

	protected abstract ModelExecuteOptions createModelOptions();

	protected void updateResourceObjectStatusPerformed(ShadowType selected, AjaxRequestTarget target,
			boolean enabled) {
		List<ShadowType> selectedShadow = null;
		if (selected != null) {
			selectedShadow = new ArrayList<>();
			selectedShadow.add(selected);
		} else {
			selectedShadow = getTable().getSelectedObjects();
		}

		OperationResult result = new OperationResult(OPERATION_UPDATE_STATUS);
		Task task = pageBase.createSimpleTask(OPERATION_UPDATE_STATUS);

		if (selectedShadow == null || selectedShadow.isEmpty()) {
			result.recordWarning("Nothing selected to update status");
			getPageBase().showResult(result);
			target.add(getPageBase().getFeedbackPanel());
			return;
		}

		ModelExecuteOptions opts = createModelOptions();

		for (ShadowType shadow : selectedShadow) {
			ActivationStatusType status = enabled ? ActivationStatusType.ENABLED
					: ActivationStatusType.DISABLED;
			try {
				ObjectDelta<ShadowType> deleteDelta = ObjectDelta.createModificationReplaceProperty(
						ShadowType.class, shadow.getOid(),
						SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
						getPageBase().getPrismContext(), status);
				getPageBase().getModelService().executeChanges(
						WebComponentUtil.createDeltaCollection(deleteDelta), opts, task, result);
			} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
					| ExpressionEvaluationException | CommunicationException | ConfigurationException
					| PolicyViolationException | SecurityViolationException e) {
				// TODO Auto-generated catch block
				result.recordPartialError("Could not update status (to " + status + ") for " + shadow, e);
				LOGGER.error("Could not update status (to {}) for {}, using option {}", status, shadow, opts,
						e);
				continue;
			}
		}

		result.computeStatusIfUnknown();
		getPageBase().showResult(result);
		getTable().refreshTable(null, target);
		target.add(getPageBase().getFeedbackPanel());

	}

	private PrismObjectDefinition<FocusType> getFocusDefinition() {
		return pageBase.getPrismContext().getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(FocusType.class);
	}

	private MainObjectListPanel<ShadowType> getTable() {
		return (MainObjectListPanel<ShadowType>) get(pageBase.createComponentPath(ID_TABLE));
	}

	private void changeOwner(ShadowType selected, AjaxRequestTarget target, FocusType ownerToChange,
			Operation operation) {

		List<ShadowType> selectedShadow = null;
		if (selected != null) {
			selectedShadow = new ArrayList<>();
			selectedShadow.add(selected);
		} else {
			selectedShadow = getTable().getSelectedObjects();
		}

		Collection<? extends ItemDelta> modifications = new ArrayList<>();

		ReferenceDelta delta = null;
		switch (operation) {

			case REMOVE:
				for (ShadowType shadow : selectedShadow) {
					modifications = new ArrayList<>();
					FocusType owner = loadShadowOwner(shadow.getOid());
					if (owner != null) {
						delta = ReferenceDelta.createModificationDelete(FocusType.F_LINK_REF,
								getFocusDefinition(),
								ObjectTypeUtil.createObjectRef(shadow).asReferenceValue());

						((Collection) modifications).add(delta);
						changeOwnerInternal(owner.getOid(), modifications, target);
					}
				}
				break;
			case MODIFY:
				if (!isSatisfyConstraints(selectedShadow)) {
					break;
				}

				ShadowType shadow = selectedShadow.iterator().next();
				FocusType owner = loadShadowOwner(shadow.getOid());
				if (owner != null) {
					delta = ReferenceDelta.createModificationDelete(FocusType.F_LINK_REF,
							getFocusDefinition(), ObjectTypeUtil.createObjectRef(shadow).asReferenceValue());

					((Collection) modifications).add(delta);
					changeOwnerInternal(owner.getOid(), modifications, target);
				}
				modifications = new ArrayList<>();

				delta = ReferenceDelta.createModificationAdd(FocusType.F_LINK_REF, getFocusDefinition(),
						ObjectTypeUtil.createObjectRef(shadow).asReferenceValue());
				((Collection) modifications).add(delta);
				changeOwnerInternal(ownerToChange.getOid(), modifications, target);

				break;
		}

	}

	private boolean isSatisfyConstraints(List selected) {
		if (selected.size() > 1) {
			error("Could not link to more than one owner");
			return false;
		}

		if (selected.isEmpty()) {
			warn("Could not link to more than one owner");
			return false;
		}

		return true;
	}

	private void changeOwnerInternal(String ownerOid, Collection<? extends ItemDelta> modifications,
			AjaxRequestTarget target) {
		OperationResult result = new OperationResult(OPERATION_CHANGE_OWNER);
		Task task = pageBase.createSimpleTask(OPERATION_CHANGE_OWNER);
		ObjectDelta objectDelta = ObjectDelta.createModifyDelta(ownerOid, modifications, FocusType.class,
				pageBase.getPrismContext());
		Collection deltas = new ArrayList<>();
		deltas.add(objectDelta);
		try {
			if (!deltas.isEmpty()) {
				pageBase.getModelService().executeChanges(deltas, null, task, result);

			}
		} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
				| ExpressionEvaluationException | CommunicationException | ConfigurationException
				| PolicyViolationException | SecurityViolationException e) {

		}

		result.computeStatusIfUnknown();

		pageBase.showResult(result);
		target.add(pageBase.getFeedbackPanel());
		getTable().refreshTable(null, target);
		target.add(ResourceContentPanel.this);
	}

	protected abstract SelectorOptions<GetOperationOptions> addAdditionalOptions();

	protected abstract boolean isUseObjectCounting();

}