package ru.practicum.item;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.practicum.user.User;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@Entity
@ToString
@Table(name = "items", schema = "public")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "url", length = 1500)
    private String url;

    @Column(name = "resolved_url", length = 1500)
    private String resolvedUrl;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "title", length = 1000)
    private String title;

    @Column(name = "has_image")
    private Boolean hasImage;

    @Column(name = "has_video")
    private Boolean hasVideo;

    @Column(name = "date_resolved")
    private Instant dateResolved;

    private Boolean unread;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "tags", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "name")
    private Set<String> tags = new HashSet<>();

    public UrlMetaDataRetriever.UrlMetadata getUrlMetadata() {
        return new UrlMetaDataRetrieverImpl.UrlMetadataImpl(
                this.url,
                this.resolvedUrl,
                this.mimeType,
                this.title,
                this.hasImage,
                this.hasVideo,
                this.dateResolved
                );
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Item)) return false;
        return id != null && id.equals(((Item) obj).getId());
    }
}