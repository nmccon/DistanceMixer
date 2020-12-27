# DistanceMixer
GUI for FOA spatialiasation based on distance

The DistanceMixer is a software artefact designed to assist in the composition of fixed media works that seek to integrate environmental sounds and structures into music. It represents an attempt at merging a variety of compositional aesthetics into a single tool. It aims to fuse aspects of acoustic ecology with electroacoustic techniques by allowing the user to structure musical phrases and textures with clearly partitioned spectral and spatial niches using a basic form of mimesis to imply realistic spatial impressions and behaviour.

Dependencies:
Ambisonic Toolkit, Eli Fieldsteel's makeBufDict instance method for PathName.

Reverberation:
The example in the help documentation shows a simple convolution function. This class does not include any B-format impulse responses, the user must supply their own FOA FuMa IRs. As the calculations determining the ratio of dry and wet signals occur independently of the convolution process, the user is free to implement their own reverberation solutions, including third-party VST products, provided they operate on FuMa FOA signals.
