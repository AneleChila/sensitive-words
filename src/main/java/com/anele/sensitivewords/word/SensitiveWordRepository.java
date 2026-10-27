package com.anele.sensitivewords.word;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Set;

public interface SensitiveWordRepository extends JpaRepository<SensitiveWord, UUID> {
    boolean existsByValue(String value);
    boolean existsByValueAndIdNot(String value, UUID id);
    @Query("select w.value from SensitiveWord w")
    List<String> findAllValues();
    @Query("select w.value from SensitiveWord w where w.value in :values")
    Set<String> findValuesIn(@Param("values") Collection<String> values);
}
