/*
 * ******************************************************************************
 *
 * INTEL CONFIDENTIAL
 *
 * Copyright 2013 - 2016 Intel Corporation All Rights Reserved.
 *
 * The source code contained or described herein and all documents related to
 * the source code ("Material") are owned by Intel Corporation or its suppliers
 * or licensors. Title to the Material remains with Intel Corporation or its
 * suppliers and licensors. The Material contains trade secrets and proprietary
 * and confidential information of Intel or its suppliers and licensors. The
 * Material is protected by worldwide copyright and trade secret laws and treaty
 * provisions. No part of the Material may be used, copied, reproduced,
 * modified, published, uploaded, posted, transmitted, distributed, or disclosed
 * in any way without Intel's prior express written permission.
 *
 * No license under any patent, copyright, trade secret or other intellectual
 * property right is granted to or conferred upon you by disclosure or delivery
 * of the Materials, either expressly, by implication, inducement, estoppel or
 * otherwise. Any license under such intellectual property rights must be
 * express and approved by Intel in writing.
 *
 * Unless otherwise agreed by Intel in writing, you may not remove or alter this
 * notice or any other notice embedded in Materials by Intel or Intel's
 * suppliers or licensors in any way.
 *
 * ******************************************************************************
 */
package com.intel.icecp.core.permissions;

import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Implement basic name and action matching for extension by descendant classes.
 *
 */
public abstract class BasePermission extends Permission {

    private final List<String> actionList;
    private final String actionString;

    /**
     * Constructor. E.g. new BasePermission("read").
     *
     * @param actions the actions to check for access, in canonical order
     * @throws IllegalArgumentException if one of the actions is not a valid
     * action for this class
     */
    public BasePermission(String actions) {
        this("*", actions);
    }

    /**
     * Constructor. E.g. new BasePermissions("/my/device/name", "read,write");
     *
     * @param name the unique name of the device
     * @param actions the actions to check for access, in canonical order
     * @throws IllegalArgumentException if one of the actions is not a valid
     * action for this class
     */
    public BasePermission(String name, String actions) {
        super(name);
        this.actionList = parseActions(actions);
        this.actionString = actions;
        checkForValidActions(actionList);
    }

    /**
     * @param actions a comma-delimited list of actions to check
     * @return a list of actions, with whitespace trimmed
     */
    private List<String> parseActions(String actions) {
        List<String> actionList = new ArrayList<>();
        for (String action : actions.split(",")) {
            actionList.add(action.trim());
        }
        return actionList;
    }

    /**
     * @param actions the list of actions to check
     * @throws IllegalArgumentException if one of the actions is not a valid
     * action for this class
     */
    private void checkForValidActions(List<String> actions) {
        List<String> validActions = Arrays.asList(getValidActions());
        for (String action : actions) {
            if (!action.equals("*") && !validActions.contains(action)) {
                throw new IllegalArgumentException("Unknown action: " + action);
            }
        }
    }

    /**
     * @return the array of valid actions for this permission; e.g. ["read",
     * "write", "close"].
     */
    public abstract String[] getValidActions();

    /**
     * {@inheritDoc}
     */
    @Override
    public String getActions() {
        return actionString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean implies(Permission permission) {
        if (permission.getClass().isInstance(this)) {
            return hasMatchingName(permission) && hasMatchingActions(permission);
        }
        return false;
    }

    /**
     * @param permission the {@link Permission} to check against
     * @return true if the names match, or this permission implies any name
     * (e.g. *)
     */
    private boolean hasMatchingName(Permission permission) {
        return getName().equals("*") || getName().equals(permission.getName())
                || hasMatchingWildcard(getName(), permission.getName());
    }

    /**
     * @param base the string, possibly with a wildcard, to compare from
     * @param other the string to compare against
     * @return true if the base has a wildcard that is satisfied by the second
     * parameter
     */
    private boolean hasMatchingWildcard(String base, String other) {
        if (base.contains("*")) {
            String substring = base.substring(0, base.indexOf("*"));
            return other.startsWith(substring);
        }
        return false;
    }

    /**
     * @param permission the {@link Permission} to check against
     * @return true if the actions match, or this permission implies any set of
     * actions (e.g. *)
     */
    private boolean hasMatchingActions(Permission permission) {
        return getActions().equals("*") || actionList.containsAll(parseActions(permission.getActions()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BasePermission) || !(this.getClass().isInstance(obj))) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        Permission permission = (Permission) obj;
        return getName().equals(permission.getName()) && getActions().equals(permission.getActions());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(getName());
        hash = 97 * hash + Objects.hashCode(getActions());
        return hash;
    }
}
