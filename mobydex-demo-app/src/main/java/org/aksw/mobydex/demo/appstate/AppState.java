package org.aksw.mobydex.demo.appstate;

import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;

import org.aksw.mobydex.demo.MainLayout;
import org.aksw.mobydex.demo.backend.ProjectDao;
import org.aksw.mobydex.demo.domain.Project;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

@RouteScope
@RouteScopeOwner(MainLayout.class)
public class AppState {

    private final BehaviorSubject<Long> selectedProject =
            BehaviorSubject.create();

    // private final Observable<Project> projectState;

    public AppState(ProjectDao projectService) {
//        projectState = selectedProject
//                .distinctUntilChanged()
//                .switchMap(id ->
//                        projectService.load(id)
//                                .map(ProjectState.Loaded::new)
//                                .toObservable()
//                                .startWithItem(new ProjectState.Loading(id))
//                                .onErrorReturn(error ->
//                                        new ProjectState.Failed(id, error)))
//                .replay(1)
//                .refCount();
    }

    public void selectProject(Long id) {
        selectedProject.onNext(id);
    }

    public Observable<Long> selectedProject() {
        return selectedProject.hide();
    }

    public Observable<Project> projectState() {
        // return projectState;
        return null;
    }
}
