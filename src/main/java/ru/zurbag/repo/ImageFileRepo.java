package ru.zurbag.repo;

import org.springframework.data.repository.CrudRepository;
import ru.zurbag.entity.ImageFile;

import java.util.List;

public interface ImageFileRepo extends CrudRepository<ImageFile, Long> {
    ImageFile getByName(String name);

    List<ImageFile> getByIsCompressed(Boolean b);
}
