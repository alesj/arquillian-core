/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.spi.client.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;

/**
 *  
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class DeploymentScenario
{
   private final List<Deployment> deployments;

   public DeploymentScenario()
   {
      this.deployments = new ArrayList<Deployment>();
   }

   public DeploymentScenario addDeployment(DeploymentDescription deployment)
   {
      Validate.notNull(deployment, "Deployment must be specified");
      this.deployments.add(new Deployment(deployment));
      return this;
   }

   public Set<TargetDescription> targets()
   {
      Set<TargetDescription> targets = new HashSet<TargetDescription>();

      for (Deployment dep : deployments)
      {
         targets.add(dep.getDescription().getTarget());
      }
      return targets;
   }

   public Set<ProtocolDescription> protocols()
   {
      Set<ProtocolDescription> protocols = new HashSet<ProtocolDescription>();

      for (Deployment dep : deployments)
      {
         protocols.add(dep.getDescription().getProtocol());
      }
      return protocols;
   }

   /** 
    * Get a {@link DeploymentDescription} with a specific name if it exists.
    * 
    * @param target The name of the {@link DeploymentDescription}
    * @return Defined Deployment or null if not found.
    */
   public Deployment deployment(DeploymentTargetDescription target)
   {
      Validate.notNull(target, "Target must be specified");
      if (DeploymentTargetDescription.DEFAULT.equals(target))
      {
         return findDefaultDeployment();
      }
      return findMatchingDeployment(target);
   }

   /**
    * @return
    */
   private Deployment findDefaultDeployment()
   {
      if (deployments.size() == 1)
      {
         return deployments.get(0);
      }
      else if (deployments.size() > 1)
      {
         // if there are only one Archive deployment, default to it
         if (archiveDeploymentCount() == 1)
         {
            for (Deployment deployment : deployments)
            {
               if (deployment.getDescription().isArchiveDeployment())
               {
                  return deployment;
               }
            }
         }
         if(managedDeploymentCount() == 1)
         {
            for (Deployment deployment : deployments)
            {
               if (deployment.getDescription().managed())
               {
                  return deployment;
               }
            }
         }
      }
      return null;
   }

   /**
    * @return
    */
   private int managedDeploymentCount()
   {
      int count = 0;
      for (Deployment deployment : deployments)
      {
         if (deployment.getDescription().managed())
         {
            count++;
         }
      }
      return count;
   }

   private int archiveDeploymentCount()
   {
      int count = 0;
      for (Deployment deployment : deployments)
      {
         if (deployment.getDescription().isArchiveDeployment())
         {
            count++;
         }
      }
      return count;
   }

   /**
    * @param target
    * @return
    */
   private Deployment findMatchingDeployment(DeploymentTargetDescription target)
   {
      for (Deployment deployment : deployments)
      {
         if (deployment.getDescription().getName().equals(target.getName()))
         {
            return deployment;
         }
      }
      return null;
   }

   public List<Deployment> managedDeploymentsInDeployOrder()
   {
      List<Deployment> managedDeployment = new ArrayList<Deployment>();
      for (Deployment deployment : deployments)
      {
         DeploymentDescription desc = deployment.getDescription();
         if (desc.managed())
         {
            managedDeployment.add(deployment);
         }
      }
      Collections.sort(managedDeployment, new Comparator<Deployment>()
      {
         public int compare(Deployment o1, Deployment o2)
         {
            return new Integer(o1.getDescription().getOrder()).compareTo(o2.getDescription().getOrder());
         }
      });

      return Collections.unmodifiableList(managedDeployment);
   }

   public List<Deployment> deployedDeploymentsInUnDeployOrder()
   {
      List<Deployment> managedDeployment = new ArrayList<Deployment>();
      for (Deployment deployment : deployments)
      {
         DeploymentDescription desc = deployment.getDescription();
         if (desc.managed() || deployment.isDeployed())
         {
            managedDeployment.add(deployment);
         }
      }
      Collections.sort(managedDeployment, new Comparator<Deployment>()
      {
         public int compare(Deployment o1, Deployment o2)
         {
            return new Integer(o2.getDescription().getOrder()).compareTo(o1.getDescription().getOrder());
         }
      });

      return Collections.unmodifiableList(managedDeployment);
   }

   /**
    * Get all {@link DeploymentDescription} defined to be deployed during Test startup for a specific {@link TargetDescription} ordered.
    * 
    * @param target The Target to filter on
    * @return A List of found {@link DeploymentDescription}. Will return a empty list if none are found.
    */
   public List<Deployment> startupDeploymentsFor(TargetDescription target)
   {
      Validate.notNull(target, "Target must be specified");
      List<Deployment> startupDeployments = new ArrayList<Deployment>();
      for (Deployment deployment : deployments)
      {
         DeploymentDescription desc = deployment.getDescription();
         if (desc.managed() && target.equals(desc.getTarget()))
         {
            startupDeployments.add(deployment);
         }
      }
      // sort them by order
      Collections.sort(startupDeployments, new Comparator<Deployment>()
      {
         public int compare(Deployment o1, Deployment o2)
         {
            return new Integer(o1.getDescription().getOrder()).compareTo(o2.getDescription().getOrder());
         }
      });

      return Collections.unmodifiableList(startupDeployments);
   }

   public List<Deployment> deploymentsInError()
   {
      List<Deployment> result = new ArrayList<Deployment>();
      for (Deployment dep : this.deployments)
      {
         if (dep.hasDeploymentError())
         {
            result.add(dep);
         }
      }
      return result;
   }

   public List<Deployment> deployedDeployments()
   {
      List<Deployment> result = new ArrayList<Deployment>();
      for (Deployment dep : this.deployments)
      {
         if (dep.isDeployed())
         {
            result.add(dep);
         }
      }
      return result;
   }

   /**
    * @return the deployments
    */
   public List<Deployment> deployments()
   {
      return Collections.unmodifiableList(deployments);
   }
}
