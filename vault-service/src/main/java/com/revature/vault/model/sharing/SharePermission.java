package com.revature.vault.model.sharing;

public enum SharePermission {
    /** Password can be viewed exactly once, then the share is exhausted */
    VIEW_ONCE,
    /** Password can be viewed multiple times up to maxViews */
    VIEW_MULTIPLE,
    /** Password visible until expiry, unlimited views */
    TEMPORARY_ACCESS
}
