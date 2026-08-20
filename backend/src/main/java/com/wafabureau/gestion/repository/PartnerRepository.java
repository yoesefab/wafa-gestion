package com.wafabureau.gestion.repository;
import com.wafabureau.gestion.model.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface PartnerRepository<T extends PartnerEntity>
        extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
}
