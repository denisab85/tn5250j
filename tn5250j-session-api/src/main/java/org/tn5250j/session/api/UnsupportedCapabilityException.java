package org.tn5250j.session.api;

public class UnsupportedCapabilityException extends UnsupportedOperationException {

    private static final long serialVersionUID = 1L;

    public UnsupportedCapabilityException(Class<?> capability) {
        super("Capability not available: " + capability.getName());
    }
}
