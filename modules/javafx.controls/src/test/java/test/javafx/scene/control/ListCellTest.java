/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package test.javafx.scene.control;

import static javafx.scene.control.ControlShim.installDefaultSkin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertListenerListContains;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertListenerListDoesNotContain;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertValueListenersContains;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertValueListenersDoesNotContain;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.getInvalidationListener;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.getListChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.FocusModel;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListCellShim;
import javafx.scene.control.ListView;
import javafx.scene.control.ListView.EditEvent;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.MultipleSelectionModelBaseShim;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.skin.ListCellSkin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

/**
 */
public class ListCellTest {
    private ListCell<String> cell;
    private ListView<String> list;
    private ObservableList<String> model;
    private StageLoader stageLoader;

    @BeforeEach
    public void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });

        cell = new ListCell<>();
        model = FXCollections.observableArrayList("Apples", "Oranges", "Pears");
        list = new ListView<>(model);
    }

    @AfterEach
    public void cleanup() {
        if (stageLoader != null) stageLoader.dispose();
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void styleClassIs_list_cell_byDefault() {
        assertStyleClassContains(cell, "list-cell");
    }

    // The item should be null by default because the index is -1 by default
    @Test public void itemIsNullByDefault() {
        assertNull(cell.getItem());
    }

    /*********************************************************************
     * Tests for the listView property                                   *
     ********************************************************************/

    @Test public void listViewIsNullByDefault() {
        assertNull(cell.getListView());
        assertNull(cell.listViewProperty().get());
    }

    @Test public void updateListViewUpdatesListView() {
        cell.updateListView(list);
        assertSame(list, cell.getListView());
        assertSame(list, cell.listViewProperty().get());
    }

    @Test public void canSetListViewBackToNull() {
        cell.updateListView(list);
        cell.updateListView(null);
        assertNull(cell.getListView());
        assertNull(cell.listViewProperty().get());
    }

    @Test public void listViewPropertyReturnsCorrectBean() {
        assertSame(cell, cell.listViewProperty().getBean());
    }

    @Test public void listViewPropertyNameIs_listView() {
        assertEquals("listView", cell.listViewProperty().getName());
    }

    @Test public void updateListViewWithNullFocusModelResultsInNoException() {
        cell.updateListView(list);
        list.setFocusModel(null);
        cell.updateListView(new ListView());
    }

    @Test public void updateListViewWithNullFocusModelResultsInNoException2() {
        list.setFocusModel(null);
        cell.updateListView(list);
        cell.updateListView(new ListView());
    }

    @Test public void updateListViewWithNullFocusModelResultsInNoException3() {
        cell.updateListView(list);
        ListView list2 = new ListView();
        list2.setFocusModel(null);
        cell.updateListView(list2);
    }

    @Test public void updateListViewWithNullSelectionModelResultsInNoException() {
        cell.updateListView(list);
        list.setSelectionModel(null);
        cell.updateListView(new ListView());
    }

    @Test public void updateListViewWithNullSelectionModelResultsInNoException2() {
        list.setSelectionModel(null);
        cell.updateListView(list);
        cell.updateListView(new ListView());
    }

    @Test public void updateListViewWithNullSelectionModelResultsInNoException3() {
        cell.updateListView(list);
        ListView list2 = new ListView();
        list2.setSelectionModel(null);
        cell.updateListView(list2);
    }

    @Test public void updateListViewWithNullItemsResultsInNoException() {
        cell.updateListView(list);
        list.setItems(null);
        cell.updateListView(new ListView());
    }

    @Test public void updateListViewWithNullItemsResultsInNoException2() {
        list.setItems(null);
        cell.updateListView(list);
        cell.updateListView(new ListView());
    }

    @Test public void updateListViewWithNullItemsResultsInNoException3() {
        cell.updateListView(list);
        ListView list2 = new ListView();
        list2.setItems(null);
        cell.updateListView(list2);
    }

    /*********************************************************************
     * Tests for the item property. It should be updated whenever the    *
     * index, or listView changes, including the listView's items.       *
     ********************************************************************/

    @Test public void itemMatchesIndexWithinListItems() {
        cell.updateIndex(0);
        cell.updateListView(list);
        assertSame(model.get(0), cell.getItem());
        cell.updateIndex(1);
        assertSame(model.get(1), cell.getItem());
    }

    @Test public void itemMatchesIndexWithinListItems2() {
        cell.updateListView(list);
        cell.updateIndex(0);
        assertSame(model.get(0), cell.getItem());
        cell.updateIndex(1);
        assertSame(model.get(1), cell.getItem());
    }

    @Test public void itemIsNullWhenIndexIsOutOfRange() {
        cell.updateIndex(50);
        cell.updateListView(list);
        assertNull(cell.getItem());
    }

    @Test public void itemIsNullWhenIndexIsOutOfRange2() {
        cell.updateListView(list);
        cell.updateIndex(50);
        assertNull(cell.getItem());
    }

    // Above were the simple tests. Now we check various circumstances
    // to make sure the item is updated correctly.

    @Test public void itemIsUpdatedWhenItWasOutOfRangeButUpdatesToListViewItemsMakesItInRange() {
        cell.updateIndex(4);
        cell.updateListView(list);
        model.addAll("Pumpkin", "Lemon");
        assertSame(model.get(4), cell.getItem());
    }

    @Test public void itemIsUpdatedWhenItWasInRangeButUpdatesToListViewItemsMakesItOutOfRange() {
        cell.updateIndex(2);
        cell.updateListView(list);
        assertSame(model.get(2), cell.getItem());
        model.remove(2);
        assertNull(cell.getItem());
    }

    @Test public void itemIsUpdatedWhenListViewItemsIsUpdated() {
        cell.updateIndex(1);
        cell.updateListView(list);
        assertSame(model.get(1), cell.getItem());
        model.set(1, "Lime");
        assertEquals("Lime", cell.getItem());
    }

    @Test public void itemIsUpdatedWhenListViewItemsHasNewItemInsertedBeforeIndex() {
        cell.updateIndex(1);
        cell.updateListView(list);
        assertSame(model.get(1), cell.getItem());
        String previous = model.get(0);
        model.add(0, "Lime");
        assertEquals(previous, cell.getItem());
    }

    @Test public void itemIsUpdatedWhenListViewItemsHasItemRemovedBeforeIndex() {
        cell.updateIndex(1);
        cell.updateListView(list);
        assertSame(model.get(1), cell.getItem());
        String other = model.get(2);
        model.remove(0);
        assertEquals(other, cell.getItem());
    }

    @Test public void itemIsUpdatedWhenListViewItemsIsReplaced() {
        ObservableList<String> model2 = FXCollections.observableArrayList("Water", "Juice", "Soda");
        cell.updateIndex(1);
        cell.updateListView(list);
        list.setItems(model2);
        assertEquals("Juice", cell.getItem());
    }

    @Test public void itemIsUpdatedWhenListViewIsReplaced() {
        cell.updateIndex(2);
        cell.updateListView(list);
        ObservableList<String> model2 = FXCollections.observableArrayList("Water", "Juice", "Soda");
        ListView<String> listView2 = new ListView<>(model2);
        cell.updateListView(listView2);
        assertEquals("Soda", cell.getItem());
    }

    @Test public void replaceItemsWithANull() {
        cell.updateIndex(0);
        cell.updateListView(list);
        list.setItems(null);
        assertNull(cell.getItem());
    }

    @Test public void replaceItemsWithANull_ListenersRemovedFromFormerList() {
        cell.updateIndex(0);
        cell.updateListView(list);
        ListChangeListener listener = getListChangeListener(cell, "weakItemsListener");
        assertListenerListContains(model, listener);
        list.setItems(null);
        assertListenerListDoesNotContain(model, listener);
    }

    @Test public void replaceANullItemsWithNotNull() {
        cell.updateIndex(0);
        cell.updateListView(list);
        list.setItems(null);
        ObservableList<String> model2 = FXCollections.observableArrayList("Water", "Juice", "Soda");
        list.setItems(model2);
        assertEquals("Water", cell.getItem());
    }

//---------- tests around JDK-8251941: broken cleanup with null item

    /**
     * Transition not-empty -> empty by items modification
     */
    @Test
    public void testNullItemRemoveAsLast() {
        model.add(null);
        cell.updateListView(list);
        int last = model.size() - 1;
        cell.updateIndex(last);
        model.remove(last);
        assertOffRangeState(last);
    }

    /**
     * Sanity: transition not-empty -> not empty by items modification
     */
    @Test
    public void testNullItemRemoveAsFirst() {
        int first = 0;
        model.add(first, null);
        cell.updateListView(list);
        cell.updateIndex(first);
        model.remove(first);
        assertInRangeState(first);
    }

    /**
     * Transition not-empty -> empty by updateIndex
     */
    @Test
    public void testNullItemUpdateIndexOffRange() {
        model.add(0, null);
        cell.updateListView(list);
        cell.updateIndex(0);
        // update to off range > max
        cell.updateIndex(model.size());
        assertOffRangeState(model.size());
    }

    /**
     * Transition not-empty -> empty by updateIndex
     */
    @Test
    public void testNullItemUpdateIndexNegative() {
        model.add(0, null);
        cell.updateListView(list);
        cell.updateIndex(0);
        // update to off range < 0
        cell.updateIndex(-1);
        assertOffRangeState(-1);
    }

    /**
     * Sanity: in-range null item.
     */
    @Test
    public void testNullItem() {
        // null item in range, verify state
        model.add(0, null);
        cell.updateListView(list);
        cell.updateIndex(0);
        assertInRangeState(0);
    }

    /**
     * Asserts state for the given off-range index.
     * @param index
     */
    protected void assertOffRangeState(int index) {
        assertEquals(index, cell.getIndex(), "off range index");
        assertNull(cell.getItem(), "off range cell item must be null");
        assertTrue(cell.isEmpty(), "off range cell must be empty");
    }

    /**
     * Asserts state for the given in-range index.
     * @param index
     */
    protected void assertInRangeState(int index) {
        assertEquals(index, cell.getIndex(), "in range index");
        assertEquals(model.get(index), cell.getItem(), "in range cell item must be same as model item");
        assertFalse(cell.isEmpty(), "in range cell must not be empty");
    }


    /*********************************************************************
     * Tests for the selection listener                                  *
     ********************************************************************/

    @Test public void selectionOnSelectionModelIsReflectedInCells() {
        cell.updateListView(list);
        cell.updateIndex(0);

        ListCell<String> other = new ListCell<>();
        other.updateListView(list);
        other.updateIndex(1);

        list.getSelectionModel().selectFirst();
        assertTrue(cell.isSelected());
        assertFalse(other.isSelected());
    }

    @Test public void changesToSelectionOnSelectionModelAreReflectedInCells() {
        cell.updateListView(list);
        cell.updateIndex(0);

        ListCell<String> other = new ListCell<>();
        other.updateListView(list);
        other.updateIndex(1);

        // Because the ListView is in single selection mode, calling
        // selectNext causes a loss of focus for the first cell.
        list.getSelectionModel().selectFirst();
        list.getSelectionModel().selectNext();
        assertFalse(cell.isSelected());
        assertTrue(other.isSelected());
    }

    @Test public void replacingTheSelectionModelCausesSelectionOnCellsToBeUpdated() {
        // Cell is configured to represent row 0, which is selected.
        cell.updateListView(list);
        cell.updateIndex(0);
        list.getSelectionModel().select(0);

        // Other is configured to represent row 1 which is not selected.
        ListCell<String> other = new ListCell<>();
        other.updateListView(list);
        other.updateIndex(1);

        // The replacement selection model has row 1 selected, not row 0
        MultipleSelectionModel<String> selectionModel = new SelectionModelMock();
        selectionModel.select(1);

        list.setSelectionModel(selectionModel);
        assertFalse(cell.isSelected());
        assertTrue(other.isSelected());
    }

    @Test public void changesToSelectionOnSelectionModelAreReflectedInCells_MultipleSelection() {
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        cell.updateListView(list);
        cell.updateIndex(0);

        ListCell<String> other = new ListCell<>();
        other.updateListView(list);
        other.updateIndex(1);

        list.getSelectionModel().selectFirst();
        list.getSelectionModel().selectNext();
        assertTrue(cell.isSelected());
        assertTrue(other.isSelected());
    }

    @Test public void replacingTheSelectionModelCausesSelectionOnCellsToBeUpdated_MultipleSelection() {
        // Cell is configured to represent row 0, which is selected.
        cell.updateListView(list);
        cell.updateIndex(0);
        list.getSelectionModel().select(0);

        // Other is configured to represent row 1 which is not selected.
        ListCell<String> other = new ListCell<>();
        other.updateListView(list);
        other.updateIndex(1);

        // The replacement selection model has row 0 and 1 selected
        MultipleSelectionModel<String> selectionModel = new SelectionModelMock();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
        selectionModel.selectIndices(0, 1);

        list.setSelectionModel(selectionModel);
        assertTrue(cell.isSelected());
        assertTrue(other.isSelected());
    }

    @Test public void replaceANullSelectionModel() {
        // Cell is configured to represent row 0, which is selected.
        list.setSelectionModel(null);
        cell.updateIndex(0);
        cell.updateListView(list);

        // Other is configured to represent row 1 which is not selected.
        ListCell<String> other = new ListCell<>();
        other.updateListView(list);
        other.updateIndex(1);

        // The replacement selection model has row 1 selected
        MultipleSelectionModel<String> selectionModel = new SelectionModelMock();
        selectionModel.select(1);

        list.setSelectionModel(selectionModel);
        assertFalse(cell.isSelected());
        assertTrue(other.isSelected());
    }

    @Test public void setANullSelectionModel() {
        // Cell is configured to represent row 0, which is selected.
        cell.updateIndex(0);
        cell.updateListView(list);

        // Other is configured to represent row 1 which is not selected.
        ListCell<String> other = new ListCell<>();
        other.updateListView(list);
        other.updateIndex(1);

        // Replace with a null selection model, which should clear selection
        list.setSelectionModel(null);
        assertFalse(cell.isSelected());
        assertFalse(other.isSelected());
    }

    @Test public void replacingTheSelectionModelRemovesTheListenerFromTheOldModel() {
        cell.updateIndex(0);
        cell.updateListView(list);
        MultipleSelectionModel<String> sm = list.getSelectionModel();
        ListChangeListener listener = getListChangeListener(cell, "weakSelectedListener");
        assertListenerListContains(sm.getSelectedIndices(), listener);
        list.setSelectionModel(new SelectionModelMock());
        assertListenerListDoesNotContain(sm.getSelectedIndices(), listener);
    }

    /*********************************************************************
     * Tests for the focus listener                                      *
     ********************************************************************/

    @Test public void focusOnFocusModelIsReflectedInCells() {
        cell.updateListView(list);
        cell.updateIndex(0);

        ListCell<String> other = new ListCell<>();
        other.updateListView(list);
        other.updateIndex(1);

        list.getFocusModel().focus(0);
        assertTrue(cell.isFocused());
        assertFalse(other.isFocused());
    }

    @Test public void changesToFocusOnFocusModelAreReflectedInCells() {
        cell.updateListView(list);
        cell.updateIndex(0);

        ListCell<String> other = new ListCell<>();
        other.updateListView(list);
        other.updateIndex(1);

        list.getFocusModel().focus(0);
        list.getFocusModel().focus(1);
        assertFalse(cell.isFocused());
        assertTrue(other.isFocused());
    }

    @Test public void replacingTheFocusModelCausesFocusOnCellsToBeUpdated() {
        // Cell is configured to represent row 0, which is focused.
        cell.updateListView(list);
        cell.updateIndex(0);
        list.getFocusModel().focus(0);

        // Other is configured to represent row 1 which is not focused.
        ListCell<String> other = new ListCell<>();
        other.updateListView(list);
        other.updateIndex(1);

        // The replacement focus model has row 1 selected, not row 0
        FocusModel<String> focusModel = new FocusModelMock();
        focusModel.focus(1);

        list.setFocusModel(focusModel);
        assertFalse(cell.isFocused());
        assertTrue(other.isFocused());
    }

    @Test public void replaceANullFocusModel() {
        // Cell is configured to represent row 0, which is focused.
        list.setFocusModel(null);
        cell.updateIndex(0);
        cell.updateListView(list);

        // Other is configured to represent row 1 which is not focused
        ListCell<String> other = new ListCell<>();
        other.updateListView(list);
        other.updateIndex(1);

        // The replacement focus model has row 1 focused
        FocusModel<String> focusModel = new FocusModelMock();
        focusModel.focus(1);

        list.setFocusModel(focusModel);
        assertFalse(cell.isFocused());
        assertTrue(other.isFocused());
    }

    @Test public void setANullFocusModel() {
        // Cell is configured to represent row 0, which is focused.
        cell.updateIndex(0);
        cell.updateListView(list);

        // Other is configured to represent row 1 which is not focused.
        ListCell<String> other = new ListCell<>();
        other.updateListView(list);
        other.updateIndex(1);

        // Replace with a null focus model, which should clear focused
        list.setFocusModel(null);
        assertFalse(cell.isFocused());
        assertFalse(other.isFocused());
    }

    @Test public void replacingTheFocusModelRemovesTheListenerFromTheOldModel() {
        cell.updateIndex(0);
        cell.updateListView(list);
        FocusModel<String> fm = list.getFocusModel();
        InvalidationListener listener = getInvalidationListener(cell, "weakFocusedListener");
        assertValueListenersContains(fm.focusedIndexProperty(), listener);
        list.setFocusModel(new FocusModelMock());
        assertValueListenersDoesNotContain(fm.focusedIndexProperty(), listener);
    }

    /*********************************************************************
     * Tests for all things related to editing one of these guys         *
     ********************************************************************/

    // startEdit()
    @Test public void editOnListViewResultsInEditingInCell() {
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(1);
        list.edit(1);
        assertTrue(cell.isEditing());
    }

    @Test public void editOnListViewResultsInNotEditingInCellWhenDifferentIndex() {
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(1);
        list.edit(0);
        assertFalse(cell.isEditing());
    }

    @Test public void editCellWithNullListViewResultsInNoExceptions() {
        cell.updateIndex(1);
        cell.startEdit();
    }

    @Test public void editCellOnNonEditableListDoesNothing() {
        cell.updateIndex(1);
        cell.updateListView(list);
        cell.startEdit();
        assertFalse(cell.isEditing());
        assertEquals(-1, list.getEditingIndex());
    }

    @Test public void editCellWithListResultsInUpdatedEditingIndexProperty() {
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(1);
        cell.startEdit();
        assertEquals(1, list.getEditingIndex());
    }

    @Test public void editCellFiresEventOnList() {
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(2);
        final boolean[] called = new boolean[] { false };
        list.setOnEditStart(event -> {
            called[0] = true;
        });
        cell.startEdit();
        assertTrue(called[0]);
    }

    @Test public void editCellDoesNotFireEventWhileAlreadyEditing() {
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(2);
        cell.startEdit();
        List<EditEvent<?>> events = new ArrayList<>();
        list.setOnEditStart(events::add);
        cell.startEdit();
        assertEquals(0, events.size(), "startEdit must not fire event while editing");
    }

    // commitEdit()
    @Test public void commitWhenListIsNullIsOK() {
        cell.updateIndex(1);
        cell.startEdit();
        cell.commitEdit("Watermelon");
    }

    @Test public void commitWhenListIsNotNullWillUpdateTheItemsList() {
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(1);
        cell.startEdit();
        cell.commitEdit("Watermelon");
        assertEquals("Watermelon", list.getItems().get(1));
    }

    @Test public void commitSendsEventToList() {
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(1);
        cell.startEdit();
        final boolean[] called = new boolean[] { false };
        list.setOnEditCommit(event -> {
            called[0] = true;
        });
        cell.commitEdit("Watermelon");
        assertTrue(called[0]);
    }

    @Test public void afterCommitListViewEditingIndexIsNegativeOne() {
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(1);
        cell.startEdit();
        cell.commitEdit("Watermelon");
        assertEquals(-1, list.getEditingIndex());
        assertFalse(cell.isEditing());
    }

    // cancelEdit()
    @Test public void cancelEditCanBeCalledWhileListViewIsNull() {
        cell.updateIndex(1);
        cell.startEdit();
        cell.cancelEdit();
    }

    @Test public void cancelEditFiresChangeEvent() {
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(1);
        cell.startEdit();
        final boolean[] called = new boolean[] { false };
        list.setOnEditCancel(event -> {
            called[0] = true;
        });
        cell.cancelEdit();
        assertTrue(called[0]);
    }

    @Test public void cancelSetsListViewEditingIndexToNegativeOne() {
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(1);
        cell.startEdit();
        cell.cancelEdit();
        assertEquals(-1, list.getEditingIndex());
        assertFalse(cell.isEditing());
    }

    @Test public void movingListCellEditingIndexCausesCurrentlyInEditCellToCancel() {
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(0);
        cell.startEdit();

        ListCell other = new ListCell();
        other.updateListView(list);
        other.updateIndex(1);
        list.edit(1);

        assertTrue(other.isEditing());
        assertFalse(cell.isEditing());
    }

    @Test
    public void testEditCancelEventAfterCancelOnCell() {
        list.setEditable(true);
        cell.updateListView(list);
        int editingIndex = 1;
        cell.updateIndex(editingIndex);
        list.edit(editingIndex);
        List<EditEvent<String>> events = new ArrayList<>();
        list.setOnEditCancel(events::add);
        cell.cancelEdit();
        assertEquals(1, events.size());
        assertEquals(editingIndex, events.get(0).getIndex(), "editing location of cancel event");
    }

    @Test
    public void testEditCancelEventAfterCancelOnList() {
        list.setEditable(true);
        cell.updateListView(list);
        int editingIndex = 1;
        cell.updateIndex(editingIndex);
        list.edit(editingIndex);
        List<EditEvent<String>> events = new ArrayList<>();
        list.setOnEditCancel(events::add);
        list.edit(-1);
        assertEquals(1, events.size());
        assertEquals(editingIndex, events.get(0).getIndex(), "editing location of cancel event");
    }

    @Test
    public void testEditCancelEventAfterChangeEditingIndexOnList() {
        list.setEditable(true);
        cell.updateListView(list);
        int editingIndex = 1;
        cell.updateIndex(editingIndex);
        list.edit(editingIndex);
        List<EditEvent<String>> events = new ArrayList<>();
        list.setOnEditCancel(events::add);
        list.edit(0);
        assertEquals(1, events.size());
        assertEquals(editingIndex, events.get(0).getIndex(), "editing location of cancel event");
    }

    @Test
    public void testEditCancelEventAfterCellReuse() {
        list.setEditable(true);
        cell.updateListView(list);
        int editingIndex = 1;
        cell.updateIndex(editingIndex);
        list.edit(editingIndex);
        List<EditEvent<String>> events = new ArrayList<>();
        list.setOnEditCancel(events::add);
        cell.updateIndex(0);
        assertEquals(1, events.size());
        assertEquals(editingIndex, events.get(0).getIndex(), "editing location of cancel event");
    }

    @Test
    public void testEditCancelEventAfterModifyItems() {
        list.setEditable(true);
        stageLoader = new StageLoader(list);
        int editingIndex = 1;
        list.edit(editingIndex);
        List<EditEvent<String>> events = new ArrayList<>();
        list.setOnEditCancel(events::add);
        list.getItems().add(0, "added");
        Toolkit.getToolkit().firePulse();
        assertEquals(1, events.size());
        assertEquals(editingIndex, events.get(0).getIndex(), "editing location of cancel event");
    }

    @Test
    public void testEditCancelEventAfterRemoveEditingItem() {
        list.setEditable(true);
        stageLoader = new StageLoader(list);
        int editingIndex = 1;
        list.edit(editingIndex);
        List<EditEvent<String>> events = new ArrayList<>();
        list.setOnEditCancel(events::add);
        list.getItems().remove(editingIndex);
        Toolkit.getToolkit().firePulse();
        assertEquals(-1, list.getEditingIndex(), "removing item must cancel edit on list");
        assertEquals(1, events.size());
        assertEquals(editingIndex, events.get(0).getIndex(), "editing location of cancel event");
    }

    @Test
    public void testStartEditOffRangeMustNotFireStartEdit() {
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(list.getItems().size());
        List<EditEvent<?>> events = new ArrayList<>();
        list.addEventHandler(ListView.editStartEvent(), events::add);
        cell.startEdit();
        assertFalse(cell.isEditing(), "sanity: off-range cell must not be editing");
        assertEquals(0, events.size(), "must not fire editStart");
    }

    @Test
    public void testStartEditOffRangeMustNotUpdateEditingLocation() {
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(list.getItems().size());
        cell.startEdit();
        assertFalse(cell.isEditing(), "sanity: off-range cell must not be editing");
        assertEquals(-1, list.getEditingIndex(), "list editing location must not be updated");
    }

    @Test
    public void testCommitEditMustNotFireCancel() {
        list.setEditable(true);
        // JDK-8187307: handler that resets control's editing state
        list.setOnEditCommit(e -> {
            int index = e.getIndex();
            list.getItems().set(index, e.getNewValue());
            list.edit(-1);
        });
        cell.updateListView(list);
        int editingIndex = 1;
        cell.updateIndex(editingIndex);
        list.edit(editingIndex);
        List<EditEvent<String>> events = new ArrayList<>();
        list.setOnEditCancel(events::add);
        String value = "edited";
        cell.commitEdit(value);
        assertEquals(value, list.getItems().get(editingIndex), "sanity: value committed");
        assertEquals(0, events.size(), "commit must not have fired editCancel");
    }

    // When the list view item's change and affects a cell that is editing, then what?
    // When the list cell's index is changed while it is editing, then what?


    private final class SelectionModelMock extends MultipleSelectionModelBaseShim<String> {
        @Override protected int getItemCount() {
            return model.size();
        }

        @Override protected String getModelItem(int index) {
            return model.get(index);
        }

        @Override protected void focus(int index) {
            // no op
        }

        @Override protected int getFocusedIndex() {
            return list.getFocusModel().getFocusedIndex();
        }
    }

    private final class FocusModelMock extends FocusModel {
        @Override protected int getItemCount() {
            return model.size();
        }

        @Override protected Object getModelItem(int row) {
            return model.get(row);
        }
    }

    private int rt_29923_count = 0;
    @Test public void test_rt_29923() {
        // setup test
        cell = new ListCellShim<>() {
            @Override public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                rt_29923_count++;
            }
        };
        list.getItems().setAll(null, null, null);
        cell.updateListView(list);

        rt_29923_count = 0;
        cell.updateIndex(0);
        assertNull(cell.getItem());
        assertFalse(cell.isEmpty());
        assertEquals(1, rt_29923_count);

        cell.updateIndex(1);
        assertNull(cell.getItem());
        assertFalse(cell.isEmpty());

        // This test used to be as shown below....but due to JDK-8122970, it changed
        // to the enabled code beneath. Refer to the first comment in JDK-8122970
        // for more detail, but in short we can't optimise and not call updateItem
        // when the new and old items are the same - doing so means we can end
        // up with bad bindings, etc in the individual cells (in other words,
        // even if their item has not changed, the rest of their state may have)
//        assertEquals(1, rt_29923_count);    // even though the index has changed,
//                                            // the item is the same, so we don't
//                                            // update the cell item.
        assertEquals(2, rt_29923_count);
    }

    @Test public void test_rt_33106() {
        cell.updateListView(list);
        list.setItems(null);
        cell.updateIndex(1);
    }

    @Test public void test_jdk_8151524() {
        ListCell cell = new ListCell();
        cell.setSkin(new ListCellSkin(cell));
    }

    /**
     * Test that min/max/pref height respect fixedCellSize.
     * Sanity test when fixing JDK-8246745.
     */
    @Test
    public void testListCellHeights() {
        ListCell<Object> cell =  new ListCell<>();
        ListView<Object> listView = new ListView<>();
        cell.updateListView(listView);
        installDefaultSkin(cell);
        listView.setFixedCellSize(100);
        assertEquals(listView.getFixedCellSize(), cell.prefHeight(-1), 1, "pref height must be fixedCellSize");
        assertEquals(listView.getFixedCellSize(), cell.minHeight(-1), 1, "min height must be fixedCellSize");
        assertEquals(listView.getFixedCellSize(), cell.maxHeight(-1), 1, "max height must be fixedCellSize");
    }

    @Test
    public void testChangeIndexToEditing1_jdk_8264127() {
        assertChangeIndexToEditing(0, 1);
    }

    @Test
    public void testChangeIndexToEditing2_jdk_8264127() {
        assertChangeIndexToEditing(-1, 1);
    }

    @Test
    public void testChangeIndexToEditing3_jdk_8264127() {
        assertChangeIndexToEditing(1, 0);
    }

    @Test
    public void testChangeIndexToEditing4_jdk_8264127() {
        assertChangeIndexToEditing(-1, 0);
    }



    private void assertChangeIndexToEditing(int initialCellIndex, int listEditingIndex) {
        list.getFocusModel().focus(-1);
        List<EditEvent> events = new ArrayList<>();
        list.setOnEditStart(e -> {
            events.add(e);
        });
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(initialCellIndex);
        list.edit(listEditingIndex);
        assertEquals(listEditingIndex, list.getEditingIndex(), "sanity: list editingIndex ");
        assertFalse(cell.isEditing(), "sanity: cell must not be editing");
        cell.updateIndex(listEditingIndex);
        assertEquals(listEditingIndex, cell.getIndex(), "sanity: index updated ");
        assertEquals(listEditingIndex, list.getEditingIndex(), "list editingIndex unchanged by cell");
        assertTrue(cell.isEditing());
        assertEquals(1, events.size());
    }

    @Test
    public void testChangeIndexOffEditing0_jdk_8264127() {
        assertUpdateCellIndexOffEditing(1, 0);
    }
    @Test
    public void testChangeIndexOffEditing1_jdk_8264127() {
        assertUpdateCellIndexOffEditing(1, -1);
    }
    @Test
    public void testChangeIndexOffEditing2_jdk_8264127() {
        assertUpdateCellIndexOffEditing(0, 1);
    }
    @Test
    public void testChangeIndexOffEditing3_jdk_8264127() {
        assertUpdateCellIndexOffEditing(0, -1);
    }

    public void assertUpdateCellIndexOffEditing(int editingIndex, int cancelIndex) {
        list.getFocusModel().focus(-1);
        List<EditEvent> events = new ArrayList<>();
        list.setOnEditCancel(e -> {
            events.add(e);
        });
        list.setEditable(true);
        cell.updateListView(list);
        cell.updateIndex(editingIndex);
        list.edit(editingIndex);
        assertEquals(editingIndex, list.getEditingIndex(), "sanity: list editingIndex ");
        assertTrue(cell.isEditing(), "sanity: cell must be editing");
        cell.updateIndex(cancelIndex); // change cell index to negative
        assertEquals(cancelIndex, cell.getIndex(), "sanity: index updated ");
        assertEquals(editingIndex, list.getEditingIndex(), "list editingIndex unchanged by cell");
        assertFalse(cell.isEditing(), "cell must not be editing if cell index is " + cell.getIndex());
        assertEquals(1, events.size());
    }

    @Test
    public void testMisbehavingCancelEditTerminatesEdit() {
        ListCell<String> cell = new MisbehavingOnCancelListCell<>();

        list.setEditable(true);
        cell.updateListView(list);

        int editingIndex = 1;
        int intermediate = 0;
        int notEditingIndex = -1;
        cell.updateIndex(editingIndex);
        list.edit(editingIndex);
        assertTrue(cell.isEditing(), "sanity: ");
        try {
            list.edit(intermediate);
        } catch (Exception ex) {
            // just catching to test in finally
        } finally {
            assertFalse(cell.isEditing(), "cell must not be editing");
            assertEquals(intermediate, list.getEditingIndex(), "list must be editing at intermediate index");
        }
        // test editing: second round
        // switch cell off editing by cell api
        list.edit(editingIndex);
        assertTrue(cell.isEditing(), "sanity: ");
        try {
            cell.cancelEdit();
        } catch (Exception ex) {
            // just catching to test in finally
        } finally {
            assertFalse(cell.isEditing(), "cell must not be editing");
            assertEquals(notEditingIndex, list.getEditingIndex(), "list editing must be cancelled by cell");
        }
    }

    /**
     * See also: <a href="https://bugs.openjdk.org/browse/JDK-8187314">JDK-8187314</a>.
     */
    @Test
    public void testEditCommitValueChangeIsReflectedInCell() {
        list.setEditable(true);

        cell.updateListView(list);
        list.setOnEditCommit(event -> {
            assertEquals("ABCDEF", event.getNewValue());
            // Change the underlying item.
            model.set(0, "ABCDEF [Changed]");
        });

        cell.updateIndex(0);

        assertEquals("Apples", cell.getItem());

        cell.startEdit();
        cell.commitEdit("ABCDEF");

        assertEquals("ABCDEF [Changed]", cell.getItem());
    }

    /**
     * Same index and underlying item should not cause the updateItem(..) method to be called.
     */
    @Test
    public void testSameIndexAndItemShouldNotUpdateItem() {
        AtomicInteger counter = new AtomicInteger();

        list.setCellFactory(e -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                counter.incrementAndGet();
                super.updateItem(item, empty);
            }
        });

        stageLoader = new StageLoader(list);

        counter.set(0);
        IndexedCell<String> cell = VirtualFlowTestUtils.getCell(list, 0);
        cell.updateIndex(0);

        assertEquals(0, counter.get());
    }

    /**
     * The contract of a {@link ListCell} is that isItemChanged(..)
     * is called when the index is 'changed' to the same number as the old one, to evaluate if we need to call
     * updateItem(..).
     */
    @Test
    public void testSameIndexIsItemsChangedShouldBeCalled() {
        AtomicBoolean isItemChangedCalled = new AtomicBoolean();

        list.setCellFactory(e -> new ListCell<>() {
            @Override
            protected boolean isItemChanged(String oldItem, String newItem) {
                isItemChangedCalled.set(true);
                return super.isItemChanged(oldItem, newItem);
            }
        });

        stageLoader = new StageLoader(list);

        IndexedCell<String> cell = VirtualFlowTestUtils.getCell(list, 0);
        cell.updateIndex(0);

        assertTrue(isItemChangedCalled.get());
    }

    public static class MisbehavingOnCancelListCell<T> extends ListCell<T> {
        @Override
        public void cancelEdit() {
            super.cancelEdit();
            throw new RuntimeException("violating contract");
        }
    }

}
