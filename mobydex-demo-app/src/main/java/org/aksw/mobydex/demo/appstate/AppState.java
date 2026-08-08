package org.aksw.mobydex.demo.appstate;

import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;

import org.aksw.mobydex.demo.MainLayout;
import org.aksw.mobydex.demo.backend.MobyDexRdfApi;
import org.aksw.mobydex.demo.domain.Project;
import org.apache.jena.sparql.algebra.Table;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

@RouteScope
@RouteScopeOwner(MainLayout.class)
public class AppState {

    private final MobyDexRdfApi mobyDexRdfApi;

    private final BehaviorSubject<Long> selectedProject = BehaviorSubject.create();
    private final Observable<LoadingState<Long, Project>> projectState;

    private final BehaviorSubject<Long> selectedComputation = BehaviorSubject.create();
    // private final Observable<LoadingState<Long, Project>> projectState;


    private BehaviorSubject<Table> availableTags = BehaviorSubject.create();

    private BehaviorSubject<Table> selectedTags = BehaviorSubject.create();

    public AppState(MobyDexRdfApi mobyDexRdfApi) {
        this.mobyDexRdfApi = mobyDexRdfApi;
//        long id = 1;
//        Single.just(mobyDexApi.loadProject(id))
//        .<LoadingState<Long>>map(project -> LoadingState.loaded(id, project))
//        .toObservable()
//        .startWithItem(LoadingState.loading(id))
//        .onErrorReturn(error -> LoadingState.failed(id, error));

        projectState = selectedProject
            .distinctUntilChanged()
            .switchMap(id ->
                Single.just(mobyDexRdfApi.loadProject(id))
                    .<LoadingState<Long, Project>>map(project -> LoadingState.loaded(id, project))
                    .toObservable()
                    .startWithItem(LoadingState.loading(id))
                    .onErrorReturn(error -> LoadingState.failed(id, error)))
            .replay(1)
            .refCount();
    }

    public MobyDexRdfApi getMobyDexRdfApi() {
        return mobyDexRdfApi;
    }

    public void selectProject(Long id) {
        selectedProject.onNext(id);
    }

    public BehaviorSubject<Long> selectedProject() {
        return selectedProject; //.hide();
    }

    public Observable<LoadingState<Long, Project>> projectState() {
        return projectState;
    }

    public void selectComputation(Long id) {
        selectedComputation.onNext(id);
    }

    public BehaviorSubject<Long> selectedComputation() {
        return selectedProject; //.hide();
    }

    public void setAvailableTags(Table table) {
        availableTags.onNext(table);
    }

    public Observable<Table> availableTags() {
        return availableTags;
    }

    public Observable<Table> selectedTags() {
        return selectedTags;
    }

    public void setSelectedTags(Table table) {
        selectedTags.onNext(table);
    }
}
