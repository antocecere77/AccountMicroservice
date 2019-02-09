package com.quicktutorials.learnmicroservices.AccountMicroservice.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Columns;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Entity
@Table(name="accounts")
@AllArgsConstructor
@NoArgsConstructor
public class Account {

    @Id
    @Column(name="ID")
    @NotNull @NotBlank @NotEmpty
    @Getter @Setter
    private String id;

    @Column(name="FK_USER")
    @NotNull @NotBlank @NotEmpty
    @Getter @Setter
    private String fkUser;

    @Column(name="TOTAL")
    @Getter @Setter
    private Double total;

}
