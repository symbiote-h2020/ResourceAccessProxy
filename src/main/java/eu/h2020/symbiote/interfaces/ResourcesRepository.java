/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import eu.h2020.symbiote.resources.ResourceInfo;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Transactional
public interface ResourcesRepository extends JpaRepository<ResourceInfo, String> {
    
    /**
     * This method will find a Resource instance in the database by 
     * its resourceId.
     * 
     * @param resourceId    the id of the resource
     * @return              the Resource instance
     */
    public Optional<ResourceInfo> findByResourceId(String resourceId);
  
    /**
     * This method will find Resource instances associated to a platform
     * in the database by their platformId.
     * 
     * @param platformId    the id of the platform
     * @return              the Resource instances
     */
    public List<ResourceInfo> findByPlatformId(String platformId);
  
    /**
     * This method will find (a) Resource instance(s) in the database by 
     * its(their) platformResourceId.
     * 
     * @param platformResourceId    the id of the resource in the platform
     * @return                      the Resource instance(s)
     */
    public List<ResourceInfo> findByPlatformResourceId(String platformResourceId);
}
