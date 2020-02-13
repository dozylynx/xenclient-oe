XEN_REL = "4.12"
XEN_PV = "${XEN_REL}+git${SRCPV}"
XEN_BRANCH = "stable-${XEN_REL}"
XEN_SRCREV = "${AUTOREV}"

SRCREV = "${XEN_SRCREV}"
SRC_URI = "git://xenbits.xen.org/xen.git;branch=${XEN_BRANCH};name=xen"

LIC_FILES_CHKSUM := "file://COPYING;md5=bbb4b1bdc2c3b6743da3c39d03249095"

PV = "${XEN_PV}"

S = "${WORKDIR}/git"
