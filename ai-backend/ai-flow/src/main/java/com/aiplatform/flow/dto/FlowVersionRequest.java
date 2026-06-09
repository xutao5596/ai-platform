package com.aiplatform.flow.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class FlowVersionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** LogicFlow 设计 JSON */
    private String design;
    /** LiteFlow Chain(可空) */
    private String chain;
    private String changelog;
}
