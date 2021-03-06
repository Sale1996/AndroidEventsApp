package rs.ac.uns.ftn.eventsbackend.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
@Entity
public class Cover {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long offset_x;

	private Long offset_y;

	@NotNull
	@Column(length = 500)
	private String source;

	private String fbId;

	public Cover(Long id, @NotNull String source, Long offset_x, Long offset_y, String fbId) {
		super();
		this.id = id;
		this.source = source;
		this.offset_x = offset_x;
		this.offset_y = offset_y;
		this.fbId = fbId;
	}

	public Cover() {

	}
}
