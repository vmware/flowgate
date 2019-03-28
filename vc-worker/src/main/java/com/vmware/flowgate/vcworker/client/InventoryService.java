/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vcworker.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim.binding.impl.vmodl.TypeNameImpl;
import com.vmware.vim.binding.vmodl.DynamicProperty;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.query.InvalidProperty;
import com.vmware.vim.binding.vmodl.query.PropertyCollector;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.FilterSpec;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.ObjectContent;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.ObjectSpec;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.PropertySpec;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.RetrieveOptions;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.RetrieveResult;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.SelectionSpec;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.TraversalSpec;

public class InventoryService
{
    private static final Logger logger = LoggerFactory.getLogger( InventoryService.class );

    public static final String NAME = "name";

    /*
     * Re-factor hard coded string 2014-08-07
     */
    private final static String TRAVERSE_ENTITIES = "traverseEntities";

    private final static String VIEW = "view";

    private final static String CONTAINER_VIEW = "ContainerView";

    private final static String ILLEGAL_ARG_EXP_UNIQUE_NAME = "Expect 1 but %d found for %s:%s";

    private final static String ILLEGAL_ARG_EXCEPTION_BY_NAME = "Expect no more than 1 but %d found for %s:%s";

    private InventoryService()
    {
    }

    private static class InventoryServiceHolder
    {
        private static InventoryService inventoryService = new InventoryService();
    }

    public static InventoryService getInstance()
    {
        return InventoryServiceHolder.inventoryService;
    }

    public List<ManagedObjectReference> listByType( PropertyCollector propertyCollector,
                                                    ManagedObjectReference containerView, String typeName )
    {
        PropertySpec propertySpec = new PropertySpec();
        propertySpec.setType( new TypeNameImpl( typeName ) );
        propertySpec.setAll( true );

        // PropertySpec networkPropertySpec = new PropertySpec();
        // networkPropertySpec.setType(new TypeNameImpl("Network"));
        // networkPropertySpec.setAll(true);

        // TraversalSpec traversalSpec = new TraversalSpec();
        // traversalSpec.setType(new TypeNameImpl("HostSystem"));
        // traversalSpec.setPath("network");
        // traversalSpec.setSkip(false);
        // traversalSpec.setSelectSet(null);

        TraversalSpec traversalSpec = new TraversalSpec();
        traversalSpec.setName( TRAVERSE_ENTITIES );
        traversalSpec.setPath( VIEW );
        traversalSpec.setSkip( false );
        traversalSpec.setType( new TypeNameImpl( CONTAINER_VIEW ) );

        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj( containerView );
        objectSpec.setSelectSet( new SelectionSpec[] { traversalSpec } );

        FilterSpec filterSpec = new FilterSpec();
        filterSpec.setObjectSet( new ObjectSpec[] { objectSpec } );
        filterSpec.setPropSet( new PropertySpec[] { propertySpec } );

        RetrieveOptions retrieveOptions = new RetrieveOptions();

        RetrieveResult retrieveResult = null;
        try
        {
            retrieveResult = propertyCollector.retrievePropertiesEx( new FilterSpec[] { filterSpec }, retrieveOptions );
        }
        catch ( InvalidProperty invalidProperty )
        {
            logger.error( "Invalid property", invalidProperty );
        }

        ObjectContent[] objects = null;
        if ( null != retrieveResult )
        {
            objects = retrieveResult.getObjects();
        }
        List<ManagedObjectReference> targets = new ArrayList<ManagedObjectReference>();
        if ( null != objects )
        {
            for ( ObjectContent object : objects )
            {
                targets.add( object.getObj() );
                DynamicProperty[] properties = object.getPropSet();
                if ( null != properties )
                {
                    for ( DynamicProperty property : properties )
                    {
                        switch ( property.getName() )
                        {
                            case NAME:
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }

        // logger.debug("RetrieveResult \n{}", retrieveResult);
        return targets;
    }

    public ManagedObjectReference[] findAll( PropertyCollector propertyCollector, ManagedObjectReference containerView,
                                             String typeName )
    {
        List<ManagedObjectReference> targets = listByType( propertyCollector, containerView, typeName );
        return targets.toArray( new ManagedObjectReference[targets.size()] );
    }

    public void handleObjectContent( PropertyCollector propertyCollector, ManagedObjectReference containerView,
                                     String typeName, ObjectContentCallback action )
    {
        PropertySpec propertySpec = new PropertySpec();
        propertySpec.setType( new TypeNameImpl( typeName ) );
        propertySpec.setAll( true );

        TraversalSpec traversalSpec = new TraversalSpec();
        traversalSpec.setName( TRAVERSE_ENTITIES );
        traversalSpec.setPath( VIEW );
        traversalSpec.setSkip( false );
        traversalSpec.setType( new TypeNameImpl( CONTAINER_VIEW ) );

        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj( containerView );
        objectSpec.setSelectSet( new SelectionSpec[] { traversalSpec } );

        FilterSpec filterSpec = new FilterSpec();
        filterSpec.setObjectSet( new ObjectSpec[] { objectSpec } );
        filterSpec.setPropSet( new PropertySpec[] { propertySpec } );

        RetrieveResult retrieveResult = null;
        try
        {
            retrieveResult =
                propertyCollector.retrievePropertiesEx( new FilterSpec[] { filterSpec }, new RetrieveOptions() );
        }
        catch ( InvalidProperty ex )
        {
            logger.error( "Invalid property", ex );
            return;
        }

        if ( retrieveResult == null )
        {
            logger.info( "No any result found for type {}", typeName );
            return;
        }
        // logger.debug("RetrieveResult \n{}", retrieveResult);

        ObjectContent[] objects = retrieveResult.getObjects();
        if ( ( objects == null ) || ( objects.length == 0 ) )
        {
            logger.info( "No any object found for type {}", typeName );
            return;
        }

        for ( ObjectContent object : objects )
        {
            action.handle( object );
        }
    }

    public Map<String, ManagedObjectReference> listByName( PropertyCollector propertyCollector,
                                                           ManagedObjectReference containerView, final String typeName,
                                                           final String... typeValue )
    {
        final Map<String, ManagedObjectReference> result = new HashMap<>( typeValue.length );
        handleObjectContent( propertyCollector, containerView, typeName, new ObjectContentCallback()
        {
            @Override
            public void handle( ObjectContent objectContent )
            {
                for ( DynamicProperty prop : getPropSet( objectContent ) )
                {
                    if ( NAME.equals( prop.getName() ) )
                    {
                        for ( String t : typeValue )
                        {
                            if ( t.equals( prop.getVal().toString() ) )
                            {
                                result.put( t, objectContent.getObj() );
                                return;
                            }
                        }
                    }
                }
            }
        } );
        return result;
    }

    public ManagedObjectReference getUniqueByName( PropertyCollector propertyCollector,
                                                   ManagedObjectReference containerView, final String typeName,
                                                   final String typeValue )
    {
        final Map<String, ManagedObjectReference> result =
            listByName( propertyCollector, containerView, typeName, typeValue );
        int resultSize = result.size();
        if ( resultSize != 1 )
        {
            String msg = String.format( ILLEGAL_ARG_EXP_UNIQUE_NAME, resultSize, typeName, typeValue );
            throw new IllegalArgumentException( msg );
        }
        return result.get( typeValue );
    }

    public ManagedObjectReference getOneByName( PropertyCollector propertyCollector,
                                                ManagedObjectReference containerView, final String typeName,
                                                final String typeValue )
    {
        final Map<String, ManagedObjectReference> result =
            listByName( propertyCollector, containerView, typeName, typeValue );
        int resultSize = result.size();
        if ( resultSize > 1 )
        {
            String msg = String.format( ILLEGAL_ARG_EXCEPTION_BY_NAME, resultSize, typeName, typeValue );
            throw new IllegalArgumentException( msg );
        }
        return result.get( typeValue );
    }

    public boolean exists( PropertyCollector propertyCollector, ManagedObjectReference containerView,
                           final String typeName, final String typeValue )
    {
        return ( listByName( propertyCollector, containerView, typeName, typeValue ).size() > 0 );
    }

    public abstract static class ObjectContentCallback
    {
        private static final DynamicProperty[] EMPTY_ARRAY = new DynamicProperty[0];

        public abstract void handle( ObjectContent objectContent );

        DynamicProperty[] getPropSet( PropertyCollector.ObjectContent objectContent )
        {
            DynamicProperty[] properties = objectContent.getPropSet();
            return ( properties == null ) ? EMPTY_ARRAY : properties;
        }
    }
}
