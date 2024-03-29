package android.support.v7.view.menu;

import android.content.Context;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v4.internal.view.SupportSubMenu;
import android.support.v4.util.ArrayMap;
import android.view.MenuItem;
import android.view.SubMenu;
import java.util.Iterator;
import java.util.Map;

abstract class BaseMenuWrapper<T> extends BaseWrapper<T> {
    final Context mContext;
    private Map<SupportMenuItem, MenuItem> mMenuItems;
    private Map<SupportSubMenu, SubMenu> mSubMenus;

    BaseMenuWrapper(Context context, T object) {
        super(object);
        this.mContext = context;
    }

    /* Access modifiers changed, original: final */
    public final MenuItem getMenuItemWrapper(MenuItem menuItem) {
        if (!(menuItem instanceof SupportMenuItem)) {
            return menuItem;
        }
        SupportMenuItem supportMenuItem = (SupportMenuItem) menuItem;
        if (this.mMenuItems == null) {
            this.mMenuItems = new ArrayMap();
        }
        MenuItem wrappedItem = (MenuItem) this.mMenuItems.get(menuItem);
        if (wrappedItem == null) {
            wrappedItem = MenuWrapperFactory.wrapSupportMenuItem(this.mContext, supportMenuItem);
            this.mMenuItems.put(supportMenuItem, wrappedItem);
        }
        return wrappedItem;
    }

    /* Access modifiers changed, original: final */
    public final SubMenu getSubMenuWrapper(SubMenu subMenu) {
        if (!(subMenu instanceof SupportSubMenu)) {
            return subMenu;
        }
        SupportSubMenu supportSubMenu = (SupportSubMenu) subMenu;
        if (this.mSubMenus == null) {
            this.mSubMenus = new ArrayMap();
        }
        SubMenu wrappedMenu = (SubMenu) this.mSubMenus.get(supportSubMenu);
        if (wrappedMenu == null) {
            wrappedMenu = MenuWrapperFactory.wrapSupportSubMenu(this.mContext, supportSubMenu);
            this.mSubMenus.put(supportSubMenu, wrappedMenu);
        }
        return wrappedMenu;
    }

    /* Access modifiers changed, original: final */
    public final void internalClear() {
        if (this.mMenuItems != null) {
            this.mMenuItems.clear();
        }
        if (this.mSubMenus != null) {
            this.mSubMenus.clear();
        }
    }

    /* Access modifiers changed, original: final */
    public final void internalRemoveGroup(int groupId) {
        if (this.mMenuItems != null) {
            Iterator<SupportMenuItem> iterator = this.mMenuItems.keySet().iterator();
            while (iterator.hasNext()) {
                if (groupId == ((MenuItem) iterator.next()).getGroupId()) {
                    iterator.remove();
                }
            }
        }
    }

    /* Access modifiers changed, original: final */
    public final void internalRemoveItem(int id) {
        if (this.mMenuItems != null) {
            Iterator<SupportMenuItem> iterator = this.mMenuItems.keySet().iterator();
            while (iterator.hasNext()) {
                if (id == ((MenuItem) iterator.next()).getItemId()) {
                    iterator.remove();
                    break;
                }
            }
        }
    }
}
