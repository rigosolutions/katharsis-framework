package io.katharsis.resource.registry;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import io.katharsis.module.ModuleRegistry;
import io.katharsis.repository.RepositoryMethodParameterProvider;
import io.katharsis.repository.exception.RelationshipRepositoryNotFoundException;
import io.katharsis.resource.information.ResourceInformation;
import io.katharsis.resource.registry.repository.AnnotatedRelationshipEntryBuilder;
import io.katharsis.resource.registry.repository.AnnotatedResourceEntry;
import io.katharsis.resource.registry.repository.DirectResponseRelationshipEntry;
import io.katharsis.resource.registry.repository.DirectResponseResourceEntry;
import io.katharsis.resource.registry.repository.ResourceEntry;
import io.katharsis.resource.registry.repository.ResponseRelationshipEntry;
import io.katharsis.resource.registry.responseRepository.RelationshipRepositoryAdapter;
import io.katharsis.resource.registry.responseRepository.ResourceRepositoryAdapter;

/**
 * Holds information about a resource of type <i>T</i> and its repositories.
 * It includes the following information:
 * - ResourceInformation instance with information about the resource,
 * - ResourceEntry instance,
 * - List of all repositories for relationships defined in resource class.
 * - Parent RegistryEntry if a resource inherits from another resource
 *
 * @param <T> resource type
 */
public class RegistryEntry<T> {
    private final ResourceInformation resourceInformation;
    private final ResourceEntry<T, ?> resourceEntry;
    private final List<ResponseRelationshipEntry<T, ?>> relationshipEntries;
    private RegistryEntry parentRegistryEntry = null;
    
	private ModuleRegistry moduleRegistry;

    public RegistryEntry(ResourceInformation resourceInformation,
                         @SuppressWarnings("SameParameterValue") ResourceEntry<T, ?> resourceEntry) {
        this(resourceInformation, resourceEntry, new LinkedList<ResponseRelationshipEntry<T, ?>>());
    }

    public RegistryEntry(ResourceInformation resourceInformation,
                         ResourceEntry<T, ?> resourceEntry,
                         List<ResponseRelationshipEntry<T, ?>> relationshipEntries) {
        this.resourceInformation = resourceInformation;
        this.resourceEntry = resourceEntry;
        this.relationshipEntries = relationshipEntries;
    }
    
    protected void initialize(ModuleRegistry moduleRegistry){
    	this.moduleRegistry = moduleRegistry;
    }

    @SuppressWarnings("unchecked")
    public <I extends Serializable> ResourceRepositoryAdapter<T, I> getResourceRepository(RepositoryMethodParameterProvider parameterProvider) {
        Object repoInstance = null;
        if (resourceEntry instanceof DirectResponseResourceEntry) {
            repoInstance = ((DirectResponseResourceEntry<T, ?>) resourceEntry).getResourceRepository();
        } else if (resourceEntry instanceof AnnotatedResourceEntry) {
            repoInstance = ((AnnotatedResourceEntry<T, ?>) resourceEntry).build(parameterProvider);
        }
        
        if(repoInstance instanceof ResourceRegistryAware){
        	((ResourceRegistryAware)repoInstance).setResourceRegistry(moduleRegistry.getResourceRegistry());
        }

        return new ResourceRepositoryAdapter(resourceInformation, moduleRegistry, repoInstance);
    }

    public List<ResponseRelationshipEntry<T, ?>> getRelationshipEntries() {
        return relationshipEntries;
    }

    @SuppressWarnings("unchecked")
    public <D, I extends Serializable, J extends Serializable> RelationshipRepositoryAdapter<T, I, D, J> getRelationshipRepositoryForClass(Class<D> clazz,
                                                                                     RepositoryMethodParameterProvider parameterProvider) {
        ResponseRelationshipEntry<T, ?> foundRelationshipEntry = null;
        for (ResponseRelationshipEntry<T, ?> relationshipEntry : relationshipEntries) {
            if (clazz == relationshipEntry.getTargetAffiliation()) {
                foundRelationshipEntry = relationshipEntry;
                break;
            }
        }
        if (foundRelationshipEntry == null) {
            throw new RelationshipRepositoryNotFoundException(resourceInformation.getResourceClass(), clazz);
        }

        Object repoInstance;
        if (foundRelationshipEntry instanceof AnnotatedRelationshipEntryBuilder) {
            repoInstance = ((AnnotatedRelationshipEntryBuilder<T, ?>) foundRelationshipEntry).build(parameterProvider);
        } else {
             repoInstance = ((DirectResponseRelationshipEntry<T, ?>) foundRelationshipEntry).getRepositoryInstanceBuilder();
        }
         
        if(repoInstance instanceof ResourceRegistryAware){
        	((ResourceRegistryAware)repoInstance).setResourceRegistry(moduleRegistry.getResourceRegistry());
        }
         
        return new RelationshipRepositoryAdapter(resourceInformation, moduleRegistry, repoInstance);
    }

    public ResourceInformation getResourceInformation() {
        return resourceInformation;
    }

    public RegistryEntry getParentRegistryEntry() {
        return parentRegistryEntry;
    }

    /**
     * To be used only by ResourceRegistryBuilder
     *
     * @param parentRegistryEntry parent resource
     */
    void setParentRegistryEntry(RegistryEntry parentRegistryEntry) {
        this.parentRegistryEntry = parentRegistryEntry;
    }

    /**
     * Check the parameter is a parent of <b>this</b> {@link RegistryEntry} instance
     *
     * @param registryEntry parent to check
     * @return true if the parameter is a parent
     */
    public boolean isParent(RegistryEntry registryEntry) {
        RegistryEntry parentRegistryEntry = getParentRegistryEntry();
        while (parentRegistryEntry != null) {
            if (parentRegistryEntry.equals(registryEntry)) {
                return true;
            }
            parentRegistryEntry = parentRegistryEntry.getParentRegistryEntry();
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof RegistryEntry)){
            return false;
        }
        RegistryEntry<?> that = (RegistryEntry<?>) o;
        return Objects.equals(resourceInformation, that.resourceInformation) &&  // NOSONAR
            Objects.equals(resourceEntry, that.resourceEntry) &&
            Objects.equals(moduleRegistry, that.moduleRegistry) &&
            Objects.equals(relationshipEntries, that.relationshipEntries) &&
            Objects.equals(parentRegistryEntry, that.parentRegistryEntry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceInformation, resourceEntry, relationshipEntries, moduleRegistry, parentRegistryEntry);
    }
}
