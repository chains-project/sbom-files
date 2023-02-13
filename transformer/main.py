from argparse import ArgumentParser
from specs import SpecType

parser = ArgumentParser()

parser.add_argument('-s', '--spec-type', type=SpecType, choices=list(SpecType), help='choose from the specified set', required=True)
parser.add_argument('-i', '--input', type=str, help='SBOM file to be converted', required=True)
parser.add_argument('-o', '--output', type=str, help='path to output file. defaults to stdout.', required=False)

opts = parser.parse_args()


if opts.spec_type == SpecType.CYCLONEDX:
    from cyclonedx import CycloneDXTransformer
    transformer = CycloneDXTransformer(opts.input, opts.output)
    transformer.transform()

elif opts.spec_type == SpecType.JBOM:
    from jbom import JBOMTransformer
    transformer = JBOMTransformer(opts.input, opts.output)
    transformer.transform()

elif opts.spec_type == SpecType.SPDX:
    from spdx import SPDXTranformer
    transformer = SPDXTranformer(opts.input, opts.output)
    transformer.transform()

elif opts.spec_type == SpecType.SWID:
    from swid import SWIDTransformer
    transformer = SWIDTransformer(opts.input, opts.output)
    transformer.transform()

elif opts.spec_type == SpecType.SYFT:
    from syft import SyftTransformer
    transformer = SyftTransformer(opts.input, opts.output)
    transformer.transform()

elif opts.spec_type == SpecType.SCANCODE:
    from scancode import ScancodeTransformer
    transformer = ScancodeTransformer(opts.input, opts.output)
    transformer.transform()

elif opts.spec_type == SpecType.ORT:
    from ort import ORTTransformer
    transformer = ORTTransformer(opts.input, opts.output)
    transformer.transform()

elif opts.spec_type == SpecType.SPDX_RDF:
    from spdx_rdf import SPDXRDFTransformer
    transformer = SPDXRDFTransformer(opts.input, opts.output)
    transformer.transform()

elif opts.spec_type == SpecType.OPENREWRITE:
    from openrewrite import OpenRewriteTransformer
    transformer = OpenRewriteTransformer(opts.input, opts.output)
    transformer.transform()

else:
    raise ValueError('Invalid spec type')
