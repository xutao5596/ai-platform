package com.aiplatform.flow.controller;

import com.aiplatform.common.api.Result;
import com.aiplatform.flow.dto.NodeDefinitionVO;
import com.aiplatform.flow.service.NodeDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/flow/node-definitions")
@RequiredArgsConstructor
public class NodeDefinitionController {

    private final NodeDefinitionService nodeDefService;

    @GetMapping
    public Result<List<NodeDefinitionVO>> list() {
        return Result.ok(nodeDefService.listAll());
    }

    @GetMapping("/by-category")
    public Result<Map<String, List<NodeDefinitionVO>>> byCategory() {
        return Result.ok(nodeDefService.listByCategory());
    }
}
