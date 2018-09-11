package com.structurizr.view;

import com.structurizr.model.Element;
import com.structurizr.model.Person;
import com.structurizr.model.Relationship;
import com.structurizr.model.SoftwareSystem;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The superclass for all static views (system landscape, system context, container and component views).
 */
public abstract class StaticView extends View {

    private List<Animation> animations = new ArrayList<>();

    StaticView() {
    }

    StaticView(SoftwareSystem softwareSystem, String key, String description) {
        super(softwareSystem, key, description);
    }

    /**
     * Adds all software systems in the model to this view.
     */
    public void addAllSoftwareSystems() {
        getModel().getSoftwareSystems().forEach(this::add);
    }

    /**
     * Adds the given software system to this view.
     *
     * @param softwareSystem the SoftwareSystem to add
     */
    public void add(@Nonnull SoftwareSystem softwareSystem) {
        addElement(softwareSystem, true);
    }

    /**
     * Removes the given software system from this view.
     *
     * @param softwareSystem the SoftwareSystem to remove
     */
    public void remove(@Nonnull SoftwareSystem softwareSystem) {
        removeElement(softwareSystem);
    }

    /**
     * Adds all people in the model to this view.
     */
    public void addAllPeople() {
        getModel().getPeople().forEach(this::add);
    }

    /**
     * Adds the given person to this view.
     *
     * @param person the Person to add
     */
    public void add(@Nonnull Person person) {
        addElement(person, true);
    }

    /**
     * Removes the given person from this view.
     *
     * @param person the Person to add
     */
    public void remove(@Nonnull Person person) {
        removeElement(person);
    }

    /**
     * Adds a specific relationship to this view.
     *
     * @param relationship  the Relationship to be added
     * @return  a RelationshipView object representing the relationship added
     */
    public RelationshipView add(@Nonnull Relationship relationship) {
        return addRelationship(relationship);
    }

    /**
     * Adds all of the permitted elements to this view.
     */
    public abstract void addAllElements();

    /**
     * Adds all of the permitted elements, which are directly connected to the specified element, to this view.
     *
     * @param element   an Element
     */
    public abstract void addNearestNeighbours(@Nonnull Element element);

    protected <T extends Element> void addNearestNeighbours(Element element, Class<T> typeOfElement) {
        if (element == null) {
            return;
        }

        addElement(element, true);

        Set<Relationship> relationships = getModel().getRelationships();
        relationships.stream().filter(r -> r.getSource().equals(element) && typeOfElement.isInstance(r.getDestination()))
                .map(Relationship::getDestination)
                .forEach(d -> addElement(d, true));

        relationships.stream().filter(r -> r.getDestination().equals(element) && typeOfElement.isInstance(r.getSource()))
                .map(Relationship::getSource)
                .forEach(s -> addElement(s, true));
    }

    /**
     * Removes all elements that cannot be reached by traversing the graph of relationships
     * starting with the specified element.
     *
     * @param element the starting element
     */
    public void removeElementsThatAreUnreachableFrom(Element element) {
        if (element != null) {
            Set<Element> elementsToShow = new HashSet<>();
            Set<Element> elementsVisited = new HashSet<>();
            findElementsToShow(element, element, elementsToShow, elementsVisited);

            for (ElementView elementView : getElements()) {
                if (!elementsToShow.contains(elementView.getElement()) && canBeRemoved(elementView.getElement())) {
                    removeElement(elementView.getElement());
                }
            }
        }
    }

    private void findElementsToShow(Element startingElement, Element element, Set<Element> elementsToShow, Set<Element> elementsVisited) {
        if (!elementsVisited.contains(element) && getElements().contains(new ElementView(element))) {
            elementsVisited.add(element);
            elementsToShow.add(element);

            // check that we've not gone back to the starting point of the graph
            if (!element.hasEfferentRelationshipWith(startingElement)) {
                element.getRelationships().forEach(r -> findElementsToShow(startingElement, r.getDestination(), elementsToShow, elementsVisited));
            }
        }
    }

    /**
     * Removes all {@link Element}s that have the given tag from this view.
     *
     * @param tag a tag
     */
    public final void removeElementsWithTag(@Nonnull String tag) {
        getElements().stream()
                .map(ElementView::getElement)
                .filter(e -> e.hasTag(tag))
                .forEach(this::removeElement);
    }

    /**
     * Removes all {@link Relationship}s that have the given tag from this view.
     *
     * @param tag a tag
     */
    public final void removeRelationshipsWithTag(@Nonnull String tag) {
        getRelationships().stream()
                .map(RelationshipView::getRelationship)
                .filter(r -> r.hasTag(tag))
                .forEach(this::remove);
    }

    /**
     * Adds an animation step, with the specified elements.
     *
     * @param elements      the elements that should be shown in the animation step
     */
    public void addAnimation(Element... elements) {
        if (elements == null || elements.length == 0) {
            throw new IllegalArgumentException("One or more elements must be specified.");
        }

        Set<String> elementIdsInPreviousAnimationSteps = new HashSet<>();
        Set<Element> elementsInThisAnimationStep = new HashSet<>();
        Set<Relationship> relationshipsInThisAnimationStep = new HashSet<>();

        for (Element element : elements) {
            if (isElementInView(element)) {
                elementIdsInPreviousAnimationSteps.add(element.getId());
                elementsInThisAnimationStep.add(element);
            }
        }

        if (elementsInThisAnimationStep.size() == 0) {
            throw new IllegalArgumentException("None of the specified elements exist in this view.");
        }

        for (Animation animationStep : animations) {
            elementIdsInPreviousAnimationSteps.addAll(animationStep.getElements());
        }

        for (RelationshipView relationshipView : this.getRelationships()) {
            if (
                    (elementsInThisAnimationStep.contains(relationshipView.getRelationship().getSource()) && elementIdsInPreviousAnimationSteps.contains(relationshipView.getRelationship().getDestination().getId())) ||
                    (elementIdsInPreviousAnimationSteps.contains(relationshipView.getRelationship().getSource().getId()) && elementsInThisAnimationStep.contains(relationshipView.getRelationship().getDestination()))
               ) {
                relationshipsInThisAnimationStep.add(relationshipView.getRelationship());
            }
        }

        animations.add(new Animation(animations.size() + 1, elementsInThisAnimationStep, relationshipsInThisAnimationStep));
    }

    public List<Animation> getAnimations() {
        return new ArrayList<>(animations);
    }

    void setAnimations(List<Animation> animations) {
        if (animations != null) {
            this.animations = new ArrayList<>(animations);
        } else {
            this.animations = new ArrayList<>();
        }
    }

}