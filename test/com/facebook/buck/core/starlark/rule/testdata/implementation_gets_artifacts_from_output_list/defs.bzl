""" Module docstring """

def _output_list_rule_impl(ctx):
    if len(ctx.attr.outputs) != 1:
        fail("Expected one output")
    first = ctx.attr.outputs[0].short_path.replace("\\", "/")

    expected_first = "{}__/some_out.txt".format(ctx.label.name)
    if first != expected_first:
        fail("Expected short path {}, got {}".format(expected_first, first))

    # TODO(pjameson): Make sure this works properly later when multiple actions are
    #                 working
    if ctx.attr.contents:
        for output in ctx.attr.outputs:
            ctx.actions.write(output, ctx.attr.contents)

output_list_rule = rule(
    attrs = {
        "outputs": attr.output_list(),
        "contents": attr.string(),
    },
    implementation = _output_list_rule_impl,
)
