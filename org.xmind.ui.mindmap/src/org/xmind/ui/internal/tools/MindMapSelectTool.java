/* ******************************************************************************
 * Copyright (c) 2006-2008 XMind Ltd. and others.
 * 
 * This file is a part of XMind 3. XMind releases 3 and
 * above are dual-licensed under the Eclipse Public License (EPL),
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 * and the GNU Lesser General Public License (LGPL), 
 * which is available at http://www.gnu.org/licenses/lgpl.html
 * See http://www.xmind.net/license.html for details.
 * 
 * Contributors:
 *     XMind Ltd. - initial API and implementation
 *******************************************************************************/
package org.xmind.ui.internal.tools;

import static org.xmind.ui.mindmap.MindMapUI.REQ_ADD_ATTACHMENT;
import static org.xmind.ui.mindmap.MindMapUI.REQ_CREATE_BOUNDARY;
import static org.xmind.ui.mindmap.MindMapUI.REQ_CREATE_RELATIONSHIP;
import static org.xmind.ui.mindmap.MindMapUI.REQ_CREATE_SUMMARY;
import static org.xmind.ui.mindmap.MindMapUI.REQ_DRILLDOWN;
import static org.xmind.ui.mindmap.MindMapUI.REQ_DRILLUP;
import static org.xmind.ui.mindmap.MindMapUI.REQ_EDIT_LABEL;
import static org.xmind.ui.mindmap.MindMapUI.REQ_MODIFY_HYPERLINK;
import static org.xmind.ui.mindmap.MindMapUI.REQ_SELECT_CENTRAL;
import static org.xmind.ui.mindmap.MindMapUI.TOOL_CREATE_BOUNDARY;
import static org.xmind.ui.mindmap.MindMapUI.TOOL_CREATE_RELATIONSHIP;
import static org.xmind.ui.mindmap.MindMapUI.TOOL_CREATE_SUMMARY;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.xmind.core.ISheet;
import org.xmind.core.ITopic;
import org.xmind.gef.GEF;
import org.xmind.gef.IViewer;
import org.xmind.gef.Request;
import org.xmind.gef.draw2d.IOriginBased;
import org.xmind.gef.event.DragDropEvent;
import org.xmind.gef.event.KeyEvent;
import org.xmind.gef.event.MouseDragEvent;
import org.xmind.gef.event.MouseEvent;
import org.xmind.gef.graphicalpolicy.IStructure;
import org.xmind.gef.part.IGraphicalEditPart;
import org.xmind.gef.part.IGraphicalPart;
import org.xmind.gef.part.IPart;
import org.xmind.gef.service.IBendPointsFeedback;
import org.xmind.gef.service.IFeedback;
import org.xmind.gef.service.IRevealService;
import org.xmind.gef.tool.AbstractTool;
import org.xmind.gef.tool.IDragDropHandler;
import org.xmind.gef.tool.ISourceTool;
import org.xmind.gef.tool.ITool;
import org.xmind.gef.tool.SelectTool;
import org.xmind.ui.branch.IBranchDoubleClickSupport;
import org.xmind.ui.branch.IBranchMoveSupport;
import org.xmind.ui.internal.actions.GroupMarkers;
import org.xmind.ui.internal.dialogs.HyperlinkDialog2;
import org.xmind.ui.internal.mindmap.IconTipPart;
import org.xmind.ui.internal.mindmap.MindMapRevealService;
import org.xmind.ui.mindmap.IBranchPart;
import org.xmind.ui.mindmap.IBranchRangePart;
import org.xmind.ui.mindmap.IDrillDownTraceService;
import org.xmind.ui.mindmap.IImagePart;
import org.xmind.ui.mindmap.ILabelPart;
import org.xmind.ui.mindmap.ILegendItemPart;
import org.xmind.ui.mindmap.ILegendPart;
import org.xmind.ui.mindmap.IMarkerPart;
import org.xmind.ui.mindmap.IMindMap;
import org.xmind.ui.mindmap.IPlusMinusPart;
import org.xmind.ui.mindmap.IRelationshipPart;
import org.xmind.ui.mindmap.ISheetPart;
import org.xmind.ui.mindmap.ITopicPart;
import org.xmind.ui.mindmap.MindMap;
import org.xmind.ui.mindmap.MindMapUI;
import org.xmind.ui.util.MindMapUtils;
import org.xmind.ui.viewers.SWTUtils;

public class MindMapSelectTool extends SelectTool {

    private IBranchPart movingSourceBranch = null;

    public MindMapSelectTool() {
        setContextId(MindMapUI.CONTEXT_MINDMAP_EDIT);
    }

    public String getType() {
        return GEF.TOOL_SELECT;
    }

    public boolean handleMouseDown(MouseEvent me) {
        if (me.target instanceof IPlusMinusPart) {
            if (me.target.getParent() instanceof IBranchPart) {
                IBranchPart branch = (IBranchPart) me.target.getParent();
                handleMouseDownOnPlusMinus(me, branch);
                return true;
            }
        }
        return super.handleMouseDown(me);
    }

    protected void handleMouseDownOnPlusMinus(MouseEvent me, IBranchPart branch) {
        getStatus().setStatus(GEF.ST_MOUSE_PRESSED, true);
        getStatus().setStatus(GEF.ST_MOUSE_RIGHT, !me.leftOrRight);

        ITopicPart topic = branch.getTopicPart();
        if (topic != null)
            selectSingle(topic);

        sendFoldedRequest(branch);
        me.consume();
    }

    protected void sendFoldedRequest(IBranchPart branch) {
        String reqType = branch.getTopic().isFolded() ? GEF.REQ_EXTEND
                : GEF.REQ_COLLAPSE;
        Request request = new Request(reqType);
        request.setViewer(getTargetViewer());
        request.setPrimaryTarget(branch);
        request.setParameter(MindMapUI.PARAM_WITH_ANIMATION, Boolean.TRUE);
        handleRequest(request);
    }

    public boolean handleMouseUp(final MouseEvent me) {
        if (me.target instanceof IconTipPart) {
            if (me.leftOrRight) {
                getStatus().setStatus(GEF.ST_NO_DRAGGING, false);
                getStatus().setStatus(GEF.ST_MOUSE_PRESSED, false);
                if (!me.leftOrRight) {
                    getStatus().setStatus(GEF.ST_MOUSE_RIGHT, false);
                }
                IconTipPart iconTip = (IconTipPart) me.target;
                selectSingle(iconTip.getTopicPart());
                final IAction iconTipAction = iconTip.getAction();
                if (iconTipAction != null) {
                    SafeRunner.run(new SafeRunnable() {
                        public void run() throws Exception {
                            iconTipAction.run();
                            me.consume();
                        }
                    });
                    if (me.isConsumed()) {
                        setToSelectOnMouseUp(null);
                        return true;
                    }
                }
            }
        } else if (me.target instanceof IMarkerPart) {
            if (me.leftOrRight) {
                showMarkerMenu((IMarkerPart) me.target);
            }
        }
        return super.handleMouseUp(me);
    }

    protected boolean handleMouseDoubleClick(MouseEvent me) {
        if (me.leftOrRight) {
            if (me.target == null
                    || me.target == getTargetViewer().getRootPart()
                    || me.target instanceof ISheetPart) {
                IBranchPart branch = findBranch(
                        getTargetViewer().getRootPart(), me.cursorLocation);
                if (branch == null
                        || !handleDoubleClickOnBranch(branch, me.cursorLocation)) {
                    handleCreateFloatingTopicRequest(me.cursorLocation);
                }
                me.consume();
                return true;
            }
        }
        return super.handleMouseDoubleClick(me);
    }

    private IBranchPart findBranch(IPart part, Point pos) {
        if (!part.getStatus().isActive())
            return null;

        if (part instanceof IBranchPart) {
            IBranchPart branch = (IBranchPart) part;
            if (branch.getFigure().isVisible()
                    && branch.getFigure().containsPoint(pos)) {
                if (branch.canSearchChild()) {
                    for (IBranchPart sub : branch.getSubBranches()) {
                        IBranchPart b = findBranch(sub, pos);
                        if (b != null)
                            return b;
                    }
                    for (IBranchPart sub : branch.getSummaryBranches()) {
                        IBranchPart b = findBranch(sub, pos);
                        if (b != null)
                            return b;
                    }
                }
                return branch;
            }
        } else {
            if (!(part instanceof IGraphicalPart)
                    || ((IGraphicalPart) part).getFigure().isVisible()) {
                for (IPart child : part.getChildren()) {
                    IBranchPart branch = findBranch(child, pos);
                    if (branch != null)
                        return branch;
                }
            }
        }
        return null;
    }

    private boolean handleDoubleClickOnBranch(IBranchPart branch, Point pos) {
        do {
            IStructure structure = branch.getBranchPolicy()
                    .getStructure(branch);
            if (structure instanceof IBranchDoubleClickSupport) {
                if (((IBranchDoubleClickSupport) structure).handleDoubleClick(
                        branch, pos))
                    return true;
            }
            branch = branch.getParentBranch();
        } while (branch != null);
        return false;
    }

    protected boolean handleKeyDown(KeyEvent ke) {
        boolean handled = super.handleKeyDown(ke);
        if (!handled) {
            int keyCode = ke.keyCode;
            int stateMask = ke.getState();
            IPart p = getTargetViewer().getFocusedPart();
            if (p != null && p.hasRole(GEF.ROLE_EDITABLE)) {
                if (MindMapUtils.isTopicTextChar(ke.character)
                        || keyCode == 229) {
                    Request req = new Request(GEF.REQ_EDIT);
                    req.setDomain(getDomain());
                    req.setViewer(getTargetViewer());
                    startEditing(p, req);
                    if (!SWTUtils.matchKey(stateMask, keyCode, 0, ' ')) {
                        ITool activeTool = getDomain().getActiveTool();
                        if (activeTool != this) {
                            activeTool.keyDown(ke, getTargetViewer());
                        }
                    }
                    return true;
                }
            }
        }
        return handled;
    }

    protected boolean handleMouseDrag(MouseDragEvent me) {
        movingSourceBranch = null;
        return super.handleMouseDrag(me);
    }

    protected boolean canMove(IPart host, MouseDragEvent me) {
        boolean canMove = super.canMove(host, me);
        if (!canMove) {
            IBranchPart branch = findBranch(host, me.startingLocation);
            if (branch != null) {
                return canMoveInBranch(branch, me);
            }
        }
        return canMove;
    }

    private boolean canMoveInBranch(IBranchPart branch, MouseDragEvent me) {
        do {
            IStructure structure = branch.getBranchPolicy()
                    .getStructure(branch);
            if (structure instanceof IBranchMoveSupport) {
                boolean canMove = ((IBranchMoveSupport) structure).canMove(
                        branch, me);
                if (canMove) {
                    movingSourceBranch = branch;
                }
                return canMove;
            }
            branch = branch.getParentBranch();
        } while (branch != null);
        return false;
    }

    protected String getMoveTool(IPart source, MouseDragEvent me) {
        if (movingSourceBranch != null) {
            return ((IBranchMoveSupport) movingSourceBranch.getBranchPolicy()
                    .getStructure(movingSourceBranch)).getMoveTool(
                    movingSourceBranch, me);
        }
        if (source instanceof ITopicPart) {
            return MindMapUI.TOOL_MOVE_TOPIC;
        } else if (source instanceof IRelationshipPart) {
            return MindMapUI.TOOL_MOVE_RELATIONSHIP;
        } else if (source instanceof IBranchRangePart) {
            return MindMapUI.TOOL_RESIZE_RANGE;
        } else if (source instanceof IMarkerPart) {
            return MindMapUI.TOOL_MOVE_MARKER;
        } else if (source instanceof IImagePart) {
            IImagePart imagePart = (IImagePart) source;
            Object feedback = imagePart.getAdapter(IFeedback.class);
            if (feedback instanceof IBendPointsFeedback) {
                int orientation = ((IBendPointsFeedback) feedback)
                        .getOrientation(me.startingLocation);
                if (orientation == PositionConstants.NONE) {
                    return MindMapUI.TOOL_MOVE_IMAGE;
                }
            }
            return MindMapUI.TOOL_RESIZE_IMAGE;
        } else if (source instanceof ILegendPart
                || source instanceof ILegendItemPart) {
            return MindMapUI.TOOL_MOVE_LEGEND;
        }
        return super.getMoveTool(source, me);
    }

    protected void changeToMoveTool(String moveToolType, ITool moveTool,
            IPart dragSource, MouseDragEvent me) {
        if (GEF.TOOL_MOVE.equals(moveToolType)) {
            if (!canMove(dragSource, me))
                return;
            if (!dragSource.getStatus().isSelected())
                selectSingle(dragSource);
            getTargetViewer().setFocused(dragSource);
        }
        if (movingSourceBranch != null) {
            super.changeToMoveTool(moveToolType, moveTool, movingSourceBranch,
                    me);
        } else {
            super.changeToMoveTool(moveToolType, moveTool, dragSource, me);
        }
        movingSourceBranch = null;
    }

//    protected IPart findSequenceEnd(IPart current, int direction) {
//        return MindMapUtils.findNaviPart(current, direction);
//    }

    protected List<IPart> getSequenceParts(IPart start, IPart end) {
        return MindMapUtils.getSequenceTopics(start, end);
    }

    protected Request fillTargets(Request request, IViewer viewer,
            boolean includeRootPartIfEmpty) {
        return super.fillTargets(request, viewer, includeRootPartIfEmpty)
                .setParameter(MindMapUI.PARAM_WITH_ANIMATION, Boolean.TRUE);
    }

    protected void handleNonTargetedRequest(Request request) {
        IViewer viewer = request.getTargetViewer();
        if (viewer != null) {
            String requestType = request.getType();
            if (MindMapUI.REQ_CREATE_FLOAT.equals(requestType)) {
                handleCreateFloatingTopic(request, viewer);
                return;
            } else if (MindMapUI.REQ_SELECT_BROTHERS.equals(requestType)) {
                handleSelectBrothers(request);
                return;
            } else if (MindMapUI.REQ_SELECT_CHILDREN.equals(requestType)) {
                handleSelectChildren(request);
                return;
            } else if (MindMapUI.REQ_SELECT_CENTRAL.equals(requestType)) {
                handleSelectCentral(request);
                return;
            } else if (GEF.REQ_EXTEND_ALL.equals(requestType)//
                    || GEF.REQ_COLLAPSE_ALL.equals(requestType)) {
                request = createExtendOrCollapseAllRequest(request, viewer);
            } else if (REQ_SELECT_CENTRAL.equals(requestType)) {
                handleSelectCentral(viewer);
                return;
            } else if (REQ_MODIFY_HYPERLINK.equals(requestType)) {
                request = createModifyHyperlinkRequest(request, viewer);
            } else if (REQ_ADD_ATTACHMENT.equals(requestType)) {
                request = createAddAttachmentRequest(request, viewer);
            } else if (REQ_CREATE_RELATIONSHIP.equals(requestType)) {
                handleCreateRelationship(requestType, viewer);
                return;
            } else if (REQ_CREATE_BOUNDARY.equals(requestType)) {
                request = createCreateBoundaryRequest(request, viewer);
            } else if (REQ_CREATE_SUMMARY.equals(requestType)) {
                request = createCreateSummaryRequest(request, viewer);
            } else if (REQ_EDIT_LABEL.equals(requestType)) {
                handleEditLabelRequest(request, viewer);
                return;
            } else if (REQ_DRILLDOWN.equals(requestType)) {
                handleDrillDown(viewer);
                return;
            } else if (REQ_DRILLUP.equals(requestType)) {
                handleDrillUp(viewer);
                return;
            } else if (GEF.REQ_TRAVERSE.equals(requestType)) {
                handleTraverse(viewer);
                return;
            }
            if (request == null)
                return;
            if (request.hasTargets()) {
                handleTargetedRequest(request);
                return;
            }

        }
        super.handleNonTargetedRequest(request);
    }

    /**
     * @param request
     * @param viewer
     */
    private void handleCreateFloatingTopic(Request request, IViewer viewer) {
        changeActiveTool(MindMapUI.TOOL_CREATE_FLOAT);
        ITool activeTool = getDomain().getActiveTool();
        if (activeTool != null && activeTool != this) {
            activeTool.handleRequest(request);
        }
    }

    private void handleSelectCentral(Request request) {
        IViewer viewer = request.getTargetViewer();
        if (viewer == null)
            return;

        ITopicPart centralTopic = (ITopicPart) viewer
                .getAdapter(ITopicPart.class);
        if (centralTopic == null)
            return;

        IRevealService service = (IRevealService) viewer
                .getService(IRevealService.class);
        if (service instanceof MindMapRevealService) {
            ((MindMapRevealService) service).startCenteredReveal();
        }
        select(Arrays.asList(centralTopic), centralTopic);
    }

    private void handleSelectBrothers(Request request) {
        List<IPart> parts = getSelectedParts(request.getTargetViewer());
        List<ITopicPart> toSelect = new ArrayList<ITopicPart>();
        for (IPart p : parts) {
            IBranchPart branch = MindMapUtils.findBranch(p);
            if (branch != null) {
                collectBrothers(branch, toSelect);
            }
        }
        if (!toSelect.isEmpty()) {
            select(toSelect, toSelect.get(0));
        }
    }

    private void collectBrothers(IBranchPart branch, List<ITopicPart> toSelect) {
        IBranchPart parent = branch.getParentBranch();
        if (parent != null) {
            for (IBranchPart child : parent.getSubBranches()) {
                toSelect.add(child.getTopicPart());
            }
        }
    }

    private void handleSelectChildren(Request request) {
        List<IPart> parts = getSelectedParts(request.getTargetViewer());
        List<ITopicPart> toSelect = new ArrayList<ITopicPart>();
        for (IPart p : parts) {
            IBranchPart branch = MindMapUtils.findBranch(p);
            if (branch != null) {
                collectChildren(branch, toSelect);
            }
        }
        if (!toSelect.isEmpty()) {
            select(toSelect, toSelect.get(0));
        }
    }

    private void collectChildren(IBranchPart branch, List<ITopicPart> toSelect) {
        for (IBranchPart child : branch.getSubBranches()) {
            toSelect.add(child.getTopicPart());
        }
    }

    private void handleTraverse(IViewer viewer) {
        IPart focusedPart = viewer.getFocusedPart();
        if (focusedPart != null && focusedPart instanceof IGraphicalEditPart
                && focusedPart.getStatus().isActive()
                && focusedPart.hasRole(GEF.ROLE_TRAVERSABLE)) {
            IGraphicalEditPart source = (IGraphicalEditPart) focusedPart;
            ITool tool = getTool(GEF.TOOL_TRAVERSE);
            if (tool != null) {
                if (tool instanceof ISourceTool) {
                    ((ISourceTool) tool).setSource(source);
                }
                changeActiveTool(GEF.TOOL_TRAVERSE);
            }
        }
    }

    protected void handleTargetedRequest(Request request) {
        String requestType = request.getType();
        if (REQ_DRILLDOWN.equals(requestType)) {
            handleDrillDown(request.getTargetViewer(), request.getTargets());
            return;
        } else if (MindMapUI.REQ_SHOW_LEGEND.equals(requestType)) {
            if (request.getPrimaryTarget() instanceof ISheetPart) {
                ISheetPart sheet = (ISheetPart) request.getPrimaryTarget();
                if (!sheet.getSheet().getLegend().hasPosition()) {
                    createLegend(request);
                    return;
                }
            }
        }
        super.handleTargetedRequest(request);
    }

    protected void navigateTo(List<IPart> toSelect, boolean sequential) {
        List<IBranchPart> branchesToExpand = findBranchesNeedsExpand(toSelect);
        if (branchesToExpand != null && !branchesToExpand.isEmpty()) {
            expand(branchesToExpand);
        }

        if (!canNavigateTo(toSelect))
            return;

        super.navigateTo(toSelect, sequential);
    }

    protected boolean canNavigateTo(List<IPart> toSelect) {
        for (IPart p : toSelect) {
            if (p instanceof IGraphicalPart) {
                IFigure figure = ((IGraphicalPart) p).getFigure();
                if (!figure.isVisible())
                    return false;
            }
        }
        return true;
    }

    protected List<IBranchPart> findBranchesNeedsExpand(List<IPart> parts) {
        List<IBranchPart> branches = null;
        for (IPart p : parts) {
            IBranchPart branch = MindMapUtils.findBranch(p);
            if (branch != null) {
                IBranchPart toExpand = findBranchNeedsExpand(branch);
                if (toExpand != null) {
                    if (branches == null)
                        branches = new ArrayList<IBranchPart>();
                    branches.add(toExpand);
                }
            }
        }
        return branches;
    }

    private IBranchPart findBranchNeedsExpand(IBranchPart branch) {
        IBranchPart parent = branch.getParentBranch();
        if (parent != null) {
            if (parent.isFolded())
                return parent;
            return findBranchNeedsExpand(parent);
        }
        return null;
    }

    protected void expand(List<IBranchPart> branches) {
        handleSingleRequest(new Request(GEF.REQ_EXTEND).setTargets(branches));
    }

    protected Request createAddAttachmentRequest(Request request, IViewer viewer) {
        List<ITopicPart> topics = getTopicParts(getSelectedParts(viewer));
        if (topics.isEmpty())
            return request;

        FileDialog dialog = new FileDialog(viewer.getControl().getShell(),
                SWT.OPEN | SWT.MULTI);
        String ret = dialog.open();
        if (ret == null)
            return request;

        String parentPath = dialog.getFilterPath();
        String[] fileNames = dialog.getFileNames();
        List<String> paths = new ArrayList<String>(fileNames.length);
        for (String fileName : fileNames) {
            String path = new File(parentPath, fileName).getAbsolutePath();
            paths.add(path);
        }
        request.setTargets(topics);
        request.setParameter(GEF.PARAM_PATH, paths.toArray(new String[0]));
        return request;
    }

    protected Request createModifyHyperlinkRequest(Request request,
            IViewer viewer) {
        List<ITopicPart> topics = getTopicParts(getSelectedParts(viewer));
        if (topics.isEmpty())
            return null;

        if (request.hasParameter(GEF.PARAM_TEXT))
            return request.setTargets(topics);

//        HyperlinkDialog dialog;
        Shell parentShell = viewer.getControl().getShell();
        HyperlinkDialog2 dialog = new HyperlinkDialog2(parentShell,
                new StructuredSelection(topics));
//        dialog = new HyperlinkDialog2(parentShell, );

//        if (topics.size() > 1) {
//            dialog = new HyperlinkDialog2(parentShell, getHyperlink(topics));

//        HyperlinkDialog dialog = new HyperlinkDialog(parentShell,
//                getHyperlink(topics));

//        } else {
////            String initValue = getHyperlink(topics);
//         dialog = new HyperlinkDialog2(parentShell,
//                new StructuredSelection(topics));
////            dialog = new HyperlinkDialog2(parentShell, topics.get(0).getTopic()
////                    .getOwnedWorkbook(), initValue);
////            dialog = new HyperlinkDialog(parentShell, topics.get(0).getTopic()
////                    .getTitleText(), initValue);
//        }

        int retCode = dialog.open();

        if (retCode == HyperlinkDialog2.OK) {
            return request.setTargets(topics).setParameter(GEF.PARAM_TEXT,
                    dialog.getValue());

        } else if (retCode == HyperlinkDialog2.REMOVE) {
            return request.setTargets(topics)
                    .setParameter(GEF.PARAM_TEXT, null);
        }
        return null;
    }

//    private String getHyperlink(List<ITopicPart> topics) {
//        ITopicPart last = null;
//        String lastHyperlink = null;
//        for (ITopicPart t : topics) {
//            String h = t.getTopic().getHyperlink();
//            if (last != null) {
//                if (h != lastHyperlink
//                        && (h == null || !h.equals(lastHyperlink)))
//                    return null;
//            } else {
//                last = t;
//                lastHyperlink = h;
//            }
//        }
//        if (lastHyperlink != null)
//            return lastHyperlink;
//        return "http://"; //$NON-NLS-1$
//    }

    protected List<ITopicPart> getTopicParts(List<IPart> parts) {
        ArrayList<ITopicPart> topics = new ArrayList<ITopicPart>(parts.size());
        for (IPart p : parts) {
            if (p instanceof ITopicPart)
                topics.add((ITopicPart) p);
        }
        topics.trimToSize();
        return topics;
    }

    protected void handleSelectCentral(IViewer viewer) {
        ITopic centralTopic = (ITopic) viewer.getAdapter(ITopic.class);
        if (centralTopic != null) {
            viewer.setSelection(new StructuredSelection(centralTopic));
        }
    }

    protected Request createExtendOrCollapseAllRequest(Request request,
            IViewer viewer) {
        List<IPart> selectedParts = getSelectedParts(viewer);
        if (isSheetSelected(selectedParts, viewer)) {
            ISheetPart sheet = (ISheetPart) viewer.getAdapter(ISheetPart.class);
            if (sheet == null)
                return request;
            IBranchPart centralBranch = sheet.getCentralBranch();
            if (centralBranch == null)
                return request;
            selectedParts = new ArrayList<IPart>();
            selectedParts.add(centralBranch);
            selectedParts.addAll(sheet.getFloatingBranches());
        }
        request.setParameter(MindMapUI.PARAM_WITH_ANIMATION, Boolean.TRUE);
        request.setTargets(selectedParts);
        return request;
    }

    protected boolean isSheetSelected(List<IPart> parts, IViewer viewer) {
        if (parts.isEmpty())
            return true;
        for (IPart p : parts) {
            if (p instanceof ISheetPart || p == viewer.getRootPart())
                return true;
        }
        return false;
    }

    protected void handleCreateFloatingTopicRequest(Point location) {
        if (location == null)
            return;

        ISheetPart sheet = (ISheetPart) getTargetViewer().getAdapter(
                ISheetPart.class);
        if (sheet == null)
            return;

        Point position = new Point();
        IFigure figure = sheet.getFigure();
        if (figure instanceof IOriginBased) {
            Point origin = ((IOriginBased) figure).getOrigin();
            position.setLocation(location.x - origin.x, location.y - origin.y);
        }
        Request request = new Request(MindMapUI.REQ_CREATE_FLOAT);
        request.setDomain(getDomain());
        request.setViewer(getTargetViewer());
        request.setPrimaryTarget(sheet);
        request.setParameter(GEF.PARAM_POSITION, position);
        request.setParameter(MindMapUI.PARAM_WITH_ANIMATION, Boolean.TRUE);
        handleRequest(request);
    }

    protected boolean isSelectableOnSelectAll(IPart child) {
        if (!(child instanceof ITopicPart))
            return false;
        return super.isSelectableOnSelectAll(child);
    }

    protected void handleCreateRelationship(String reqType, IViewer viewer) {
        ITool next = getTool(TOOL_CREATE_RELATIONSHIP);
        if (next instanceof AbstractTool) {
            ((AbstractTool) next).setTargetViewer(viewer);
        }
        changeActiveTool(TOOL_CREATE_RELATIONSHIP);
    }

    protected Request createCreateBoundaryRequest(Request request,
            IViewer viewer) {
        List<IPart> selectedParts = getSelectedParts(viewer);
        List<ITopic> topics = MindMapUtils.getTopics(selectedParts);
        if (!topics.isEmpty()) {
            List<IPart> parts = MindMapUtils.getParts(topics, viewer);
            request.setTargets(parts);
            request.setParameter(MindMapUI.PARAM_WITH_ANIMATION, Boolean.TRUE);
            return request;
        }

        ITool next = getTool(TOOL_CREATE_BOUNDARY);
        if (next instanceof AbstractTool) {
            ((AbstractTool) next).setTargetViewer(viewer);
        }
        changeActiveTool(TOOL_CREATE_BOUNDARY);
        return null;
    }

    private Request createCreateSummaryRequest(Request request, IViewer viewer) {
        List<IPart> selectedParts = getSelectedParts(getTargetViewer());
        List<ITopic> topics = MindMapUtils.getTopics(selectedParts);
        if (!topics.isEmpty()) {
            List<IPart> parts = MindMapUtils
                    .getParts(topics, getTargetViewer());
            request.setTargets(parts);
            request.setParameter(MindMapUI.PARAM_WITH_ANIMATION, Boolean.TRUE);
            return request;
        }
        ITool next = getTool(TOOL_CREATE_SUMMARY);
        if (next instanceof AbstractTool) {
            ((AbstractTool) next).setTargetViewer(viewer);
        }
        changeActiveTool(TOOL_CREATE_SUMMARY);
        return null;
    }

    protected void handleDrillDown(IViewer viewer) {
        ISheet sheet = (ISheet) viewer.getAdapter(ISheet.class);
        if (sheet == null)
            return;
        ISelection selection = viewer.getSelection();
        if (selection instanceof IStructuredSelection) {
            Object obj = ((IStructuredSelection) selection).getFirstElement();
            if (obj instanceof ITopic) {
                ITopic rootTopic = (ITopic) obj;
                drillDown(viewer, sheet, rootTopic);
            }
        }
    }

    private void handleDrillDown(IViewer viewer, List<IPart> sources) {
        if (sources.isEmpty())
            return;

        ISheet sheet = (ISheet) viewer.getAdapter(ISheet.class);
        if (sheet == null)
            return;

        IPart p = sources.get(0);
        Object m = MindMapUtils.getRealModel(p);
        if (m instanceof ITopic) {
            drillDown(viewer, sheet, (ITopic) m);
            if (viewer.getEditDomain() != null) {
                viewer.getEditDomain().handleRequest(
                        MindMapUI.REQ_SELECT_CENTRAL, viewer);
            }
        }
    }

    private void drillDown(IViewer viewer, ISheet sheet, ITopic newRoot) {
        IMindMap newInput = new MindMap(sheet, newRoot);
        viewer.setInput(newInput);
    }

    protected void handleDrillUp(IViewer viewer) {
        ISheet sheet = (ISheet) viewer.getAdapter(ISheet.class);
        if (sheet != null) {
            IDrillDownTraceService traceService = (IDrillDownTraceService) viewer
                    .getService(IDrillDownTraceService.class);
            if (traceService != null) {
                ITopic newRoot = traceService.getPreviousCentralTopic();
                drillDown(viewer, sheet, newRoot);
            }
        }
    }

    private void createLegend(Request request) {
        changeActiveTool(MindMapUI.TOOL_CREATE_LEGEND);
    }

    protected void handleEditLabelRequest(Request request, IViewer viewer) {
        handleEditRequest(fillTargets(request, viewer, false));
    }

    protected String getEditTool(IPart source, Request request) {
        String requestType = request == null ? null : request.getType();
        if (source instanceof ITopicPart) {
            if (MindMapUI.REQ_EDIT_LABEL.equals(requestType)) {
                return MindMapUI.TOOL_EDIT_LABEL;
            } else {
                return MindMapUI.TOOL_EDIT_TOPIC_TITLE;
            }
        } else if (source instanceof ILegendItemPart) {
            return MindMapUI.TOOL_EDIT_LEGEND_ITEM;
        }
        return super.getEditTool(source, request);
    }

    protected Request createEditRequestOnDoubleClick(IPart source, MouseEvent me) {
        if (source instanceof ILabelPart) {
            ILabelPart label = (ILabelPart) source;
            IBranchPart branch = label.getOwnedBranch();
            if (branch != null) {
                ITopicPart topic = branch.getTopicPart();
                if (topic != null) {
                    return new Request(MindMapUI.REQ_EDIT_LABEL)
                            .setPrimaryTarget(topic).setDomain(getDomain())
                            .setViewer(getTargetViewer());
                }
            }
        } else if (source instanceof ILegendItemPart) {
            ILegendItemPart item = (ILegendItemPart) source;
            return new Request(MindMapUI.REQ_EDIT_LEGEND_ITEM)
                    .setPrimaryTarget(item).setDomain(getDomain()).setViewer(
                            getTargetViewer());
        }
        return super.createEditRequestOnDoubleClick(source, me);
    }

    protected boolean handleDragStarted(DragDropEvent de) {
        ITool dndTool = getTool(GEF.TOOL_DND);
        if (dndTool != null) {
            changeActiveTool(GEF.TOOL_DND);
            if (getDomain().getActiveTool() == dndTool
                    && dndTool instanceof IDragDropHandler) {
                ((IDragDropHandler) dndTool).dragStarted(de, getTargetViewer());
            }
            return true;
        }
        return super.handleDragStarted(de);
    }

    protected void showMarkerMenu(IMarkerPart target) {
        if (getTargetViewer() == null
                || getTargetViewer().getEditDomain() == null
                || getTargetViewer().getEditDomain().getCommandStack() == null)
            return;

        MenuManager menuManager = new MenuManager();
        GroupMarkers markersItem = new GroupMarkers();
        markersItem.setSourceMarkerRef(target.getMarkerRef());
        markersItem.setSelectionProvider(getTargetViewer());
        menuManager.add(markersItem);
        final Menu menu = menuManager.createContextMenu(getTargetViewer()
                .getControl());
        Point p = getTargetViewer().computeToDisplay(
                target.getFigure().getBounds().getBottomLeft(), true);
        menu.setLocation(p.x, p.y);
        menu.addMenuListener(new MenuListener() {
            public void menuShown(MenuEvent e) {
            }

            public void menuHidden(MenuEvent e) {
                e.display.asyncExec(new Runnable() {
                    public void run() {
                        menu.dispose();
                    }
                });
            }
        });
        menu.getDisplay().asyncExec(new Runnable() {
            public void run() {
                menu.setVisible(true);
            }
        });
    }

}