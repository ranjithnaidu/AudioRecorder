package com.ranjithnaidu.audiorecorder.testutils;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

public class TestUtils {
    public static final LifecycleOwner TEST_OBSERVER = new LifecycleOwner() {

        private LifecycleRegistry mRegistry = init();

        // Creates a LifecycleRegistry in RESUMED state.
        private LifecycleRegistry init() {
            LifecycleRegistry registry = new LifecycleRegistry(this);
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START);
            registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
            return registry;
        }

        @Override
        public Lifecycle getLifecycle() {
            return mRegistry;
        }
    };
}

