require xen-common.inc
require xen-tools-blktap${@bb.utils.contains('DISTRO_FEATURES', 'blktap2', '2', '3', d)}.inc
require xen-tools-openxt.inc
