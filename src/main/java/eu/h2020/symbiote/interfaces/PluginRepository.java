/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import eu.h2020.symbiote.resources.PlatformInfo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */

@Transactional
public interface PluginRepository extends JpaRepository<PlatformInfo, String> {
    /**
     * This method will find a Plugin in the database 
     * by its platformId.
     * 
     * @param platformId    the id of the platform
     * @return              the Resource instances
     */
    public Optional<PlatformInfo> findByPlatformId(String platformId);

    /**
     * This method will find a Plugin in the database 
     * by its platformName.
     * 
     * @param platformName          the name of the plugin
     * @return                      the Resource instance(s)
     */
    public Optional<PlatformInfo> findByPlatformName(String platformName);      
}
