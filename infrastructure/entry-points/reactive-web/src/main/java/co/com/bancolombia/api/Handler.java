package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.*;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.exception.NotFoundException;
import co.com.bancolombia.usecase.branch.AddBranchUseCase;
import co.com.bancolombia.usecase.branch.RenameBranchUseCase;
import co.com.bancolombia.usecase.franchise.CreateFranchiseUseCase;
import co.com.bancolombia.usecase.franchise.RenameFranchiseUseCase;
import co.com.bancolombia.model.vo.TopStockProduct;
import co.com.bancolombia.usecase.product.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class Handler {

    private final CreateFranchiseUseCase createFranchiseUseCase;
    private final RenameFranchiseUseCase renameFranchiseUseCase;
    private final AddBranchUseCase addBranchUseCase;
    private final RenameBranchUseCase renameBranchUseCase;
    private final AddProductUseCase addProductUseCase;
    private final RemoveProductUseCase removeProductUseCase;
    private final ModifyStockUseCase modifyStockUseCase;
    private final RenameProductUseCase renameProductUseCase;
    private final GetTopStockProductByBranchUseCase getTopStockProductByBranchUseCase;

    @Operation(
            tags = "Franchises",
            summary = "Create a franchise",
            description = "Creates a new franchise with the given name and no branches.",
            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CreateFranchiseRequest.class))),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Franchise created",
                            content = @Content(schema = @Schema(implementation = Franchise.class))),
                    @ApiResponse(responseCode = "400", description = "Blank, missing or too long franchise name; missing or malformed body"),
                    @ApiResponse(responseCode = "500", description = "Unexpected error")
            }
    )
    public Mono<ServerResponse> createFranchise(ServerRequest request) {
        return readBody(request, CreateFranchiseRequest.class)
                .flatMap(body -> createFranchiseUseCase.createFranchise(body.name()))
                .flatMap(franchise -> ServerResponse.status(HttpStatus.CREATED).bodyValue(franchise))
                .onErrorResume(this::handleError);
    }

    @Operation(
            tags = "Franchises",
            summary = "Rename a franchise",
            description = "Updates the name of an existing franchise.",
            parameters = {
                    @Parameter(name = "franchiseId", in = ParameterIn.PATH, required = true, description = "Franchise identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid"))
            },
            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = RenameRequest.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Franchise renamed",
                            content = @Content(schema = @Schema(implementation = Franchise.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid id; blank, missing or too long name; missing or malformed body"),
                    @ApiResponse(responseCode = "404", description = "Franchise not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected error")
            }
    )
    public Mono<ServerResponse> renameFranchise(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        return readBody(request, RenameRequest.class)
                .flatMap(body -> renameFranchiseUseCase.renameFranchise(franchiseId, body.name()))
                .flatMap(franchise -> ServerResponse.ok().bodyValue(franchise))
                .onErrorResume(this::handleError);
    }

    @Operation(
            tags = "Branches",
            summary = "Add a branch to a franchise",
            description = "Adds a new branch, with no products, to an existing franchise.",
            parameters = {
                    @Parameter(name = "franchiseId", in = ParameterIn.PATH, required = true, description = "Franchise identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid"))
            },
            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = AddBranchRequest.class))),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Branch added; returns the updated franchise",
                            content = @Content(schema = @Schema(implementation = Franchise.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid id; blank, missing or too long branch name; missing or malformed body"),
                    @ApiResponse(responseCode = "404", description = "Franchise not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected error")
            }
    )
    public Mono<ServerResponse> addBranch(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        return readBody(request, AddBranchRequest.class)
                .flatMap(body -> addBranchUseCase.addBranch(franchiseId, body.name()))
                .flatMap(franchise -> ServerResponse.status(HttpStatus.CREATED).bodyValue(franchise))
                .onErrorResume(this::handleError);
    }

    @Operation(
            tags = "Branches",
            summary = "Rename a branch",
            description = "Updates the name of a branch inside a franchise.",
            parameters = {
                    @Parameter(name = "franchiseId", in = ParameterIn.PATH, required = true, description = "Franchise identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid")),
                    @Parameter(name = "branchId", in = ParameterIn.PATH, required = true, description = "Branch identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid"))
            },
            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = RenameRequest.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Branch renamed; returns the updated franchise",
                            content = @Content(schema = @Schema(implementation = Franchise.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid id; blank, missing or too long name; missing or malformed body"),
                    @ApiResponse(responseCode = "404", description = "Franchise or branch not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected error")
            }
    )
    public Mono<ServerResponse> renameBranch(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        String branchId = request.pathVariable("branchId");
        return readBody(request, RenameRequest.class)
                .flatMap(body -> renameBranchUseCase.renameBranch(franchiseId, branchId, body.name()))
                .flatMap(franchise -> ServerResponse.ok().bodyValue(franchise))
                .onErrorResume(this::handleError);
    }

    @Operation(
            tags = "Products",
            summary = "Add a product to a branch",
            description = "Adds a new product with an initial stock to a branch of a franchise.",
            parameters = {
                    @Parameter(name = "franchiseId", in = ParameterIn.PATH, required = true, description = "Franchise identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid")),
                    @Parameter(name = "branchId", in = ParameterIn.PATH, required = true, description = "Branch identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid"))
            },
            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = AddProductRequest.class))),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Product added; returns the updated franchise",
                            content = @Content(schema = @Schema(implementation = Franchise.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid id; blank or too long name; missing or negative stock; malformed body"),
                    @ApiResponse(responseCode = "404", description = "Franchise or branch not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected error")
            }
    )
    public Mono<ServerResponse> addProduct(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        String branchId = request.pathVariable("branchId");
        return readBody(request, AddProductRequest.class)
                .flatMap(body -> addProductUseCase.addProduct(franchiseId, branchId, body.name(), body.stock()))
                .flatMap(franchise -> ServerResponse.status(HttpStatus.CREATED).bodyValue(franchise))
                .onErrorResume(this::handleError);
    }

    @Operation(
            tags = "Products",
            summary = "Remove a product from a branch",
            description = "Deletes a product from a branch of a franchise.",
            parameters = {
                    @Parameter(name = "franchiseId", in = ParameterIn.PATH, required = true, description = "Franchise identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid")),
                    @Parameter(name = "branchId", in = ParameterIn.PATH, required = true, description = "Branch identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid")),
                    @Parameter(name = "productId", in = ParameterIn.PATH, required = true, description = "Product identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Product removed; returns the updated franchise",
                            content = @Content(schema = @Schema(implementation = Franchise.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid id"),
                    @ApiResponse(responseCode = "404", description = "Franchise, branch or product not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected error")
            }
    )
    public Mono<ServerResponse> removeProduct(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        String branchId = request.pathVariable("branchId");
        String productId = request.pathVariable("productId");
        return removeProductUseCase.removeProduct(franchiseId, branchId, productId)
                .flatMap(franchise -> ServerResponse.ok().bodyValue(franchise))
                .onErrorResume(this::handleError);
    }

    @Operation(
            tags = "Products",
            summary = "Modify product stock",
            description = "Replaces the stock of a product with the given value. Stock cannot be negative.",
            parameters = {
                    @Parameter(name = "franchiseId", in = ParameterIn.PATH, required = true, description = "Franchise identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid")),
                    @Parameter(name = "branchId", in = ParameterIn.PATH, required = true, description = "Branch identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid")),
                    @Parameter(name = "productId", in = ParameterIn.PATH, required = true, description = "Product identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid"))
            },
            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ModifyStockRequest.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Stock updated; returns the updated franchise",
                            content = @Content(schema = @Schema(implementation = Franchise.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid id; missing or negative stock; missing or malformed body"),
                    @ApiResponse(responseCode = "404", description = "Franchise, branch or product not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected error")
            }
    )
    public Mono<ServerResponse> modifyStock(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        String branchId = request.pathVariable("branchId");
        String productId = request.pathVariable("productId");
        return readBody(request, ModifyStockRequest.class)
                .flatMap(body -> modifyStockUseCase.modifyStock(franchiseId, branchId, productId, body.stock()))
                .flatMap(franchise -> ServerResponse.ok().bodyValue(franchise))
                .onErrorResume(this::handleError);
    }

    @Operation(
            tags = "Products",
            summary = "Rename a product",
            description = "Updates the name of a product inside a branch.",
            parameters = {
                    @Parameter(name = "franchiseId", in = ParameterIn.PATH, required = true, description = "Franchise identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid")),
                    @Parameter(name = "branchId", in = ParameterIn.PATH, required = true, description = "Branch identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid")),
                    @Parameter(name = "productId", in = ParameterIn.PATH, required = true, description = "Product identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid"))
            },
            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = RenameRequest.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Product renamed; returns the updated franchise",
                            content = @Content(schema = @Schema(implementation = Franchise.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid id; blank, missing or too long name; missing or malformed body"),
                    @ApiResponse(responseCode = "404", description = "Franchise, branch or product not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected error")
            }
    )
    public Mono<ServerResponse> renameProduct(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        String branchId = request.pathVariable("branchId");
        String productId = request.pathVariable("productId");
        return readBody(request, RenameRequest.class)
                .flatMap(body -> renameProductUseCase.renameProduct(franchiseId, branchId, productId, body.name()))
                .flatMap(franchise -> ServerResponse.ok().bodyValue(franchise))
                .onErrorResume(this::handleError);
    }

    @Operation(
            tags = "Products",
            summary = "Get the top-stock product per branch",
            description = "Returns, for each branch of the franchise, the product with the highest stock, together with the branch it belongs to.",
            parameters = {
                    @Parameter(name = "franchiseId", in = ParameterIn.PATH, required = true, description = "Franchise identifier (canonical lowercase UUID)",
                            schema = @Schema(format = "uuid"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Top-stock product for each branch",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TopStockProduct.class)))),
                    @ApiResponse(responseCode = "400", description = "Invalid franchise id"),
                    @ApiResponse(responseCode = "404", description = "Franchise not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected error")
            }
    )
    public Mono<ServerResponse> getTopStockProducts(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        return getTopStockProductByBranchUseCase.topStockProductsByFranchise(franchiseId)
                .collectList()
                .flatMap(list -> ServerResponse.ok().bodyValue(list))
                .onErrorResume(this::handleError);
    }

    private <T> Mono<T> readBody(ServerRequest request, Class<T> bodyType) {
        return request.bodyToMono(bodyType)
                .switchIfEmpty(Mono.error(() -> new BusinessException("Request body is required")));
    }

    private Mono<ServerResponse> handleError(Throwable error) {
        if (error instanceof NotFoundException) {
            return ServerResponse.status(HttpStatus.NOT_FOUND).bodyValue(error.getMessage());
        }
        if (error instanceof BusinessException) {
            return ServerResponse.badRequest().bodyValue(error.getMessage());
        }
        // Malformed JSON, wrong field types and numeric overflow all arrive as
        // ServerWebInputException (caused by a DecodingException). Never echo its details.
        if (error instanceof ServerWebInputException) {
            return ServerResponse.badRequest().bodyValue("Invalid request body");
        }
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue("Unexpected error occurred");
    }
}
