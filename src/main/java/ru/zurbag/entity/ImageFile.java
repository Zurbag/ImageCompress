package ru.zurbag.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    private String path;
    private String name;
    private Boolean isCompressed;

    public ImageFile(String path, String name, Boolean isCompressed) {
        this.path = path;
        this.name = name;
        this.isCompressed = isCompressed;
    }
}
