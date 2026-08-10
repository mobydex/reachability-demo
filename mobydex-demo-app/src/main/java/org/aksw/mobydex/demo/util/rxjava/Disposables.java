package org.aksw.mobydex.demo.util.rxjava;

import io.reactivex.rxjava3.disposables.Disposable;

public class Disposables {
    public static void dispose(Disposable disposable) {
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
