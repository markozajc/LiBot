LiBot Runner ==========

A supplementary module that allows LiBot to be run through an IDE. Because adding all modules as runtime dependencies in the core module would cause cyclic dependencies (which Maven doesn't support even though runtime dependencies are not needed during compilation), a small top-level module adding both core and other modules is required both to have IDEs add other modules to the classpath and to support the exec:java goal in Maven (which can be called with the ./run helper binary present in the authoritative repository). 
