# WIP: IndexedCache

A queryable object store for with caching qualities.

Provides an in-memory object cache (optionally with persistence) that supports a range of queries for cached objects - 
i.e as opposed to simple key/value pair lookup.

All the hard work is done by[CQEngine](https://github.com/npgall/cqengine])and whatever[JCache](https://jcp.org/en/jsr/detail?id=107)implementation you choose to use with it. All that IndexedCache does is marry the two together so that all the features
provided by CQEngine are available against a store that can be configured with limits
for things like size and expiry where provided by the associated `JCache`. 

In theory `IndexedCache` also implicitly supports any distributed caching that is provided by the associated
`JCache` but this has not been tested and there is a caveat that is 
signficant for both distibuted and non-distributed caching in regards to objects being evicted from the
`JCache` which is described below in the section labelled "Synchronisation Limitations".

## Synchronisation Limitations

One of the roles of `IndexedCache` is to ensure that its associated `IndexedCollection` (from CQEngine)
and `JCache` implementations are kept in sync. There are two limitiations in this regard:

### 1. `JCache` Eviction

Eviction is the removal of an item from a cache because of some policy. Cache eviction is commonly
associated with cache size or the number of elements in the cache. When size or number of elements are limited and the 
limit is reached then the cache will evict elements in order to make room to store new elements.

The [JCache](https://jcp.org/en/jsr/detail?id=107) specification makes no provision for observation of cache 
eviction events. (E.g. see[https://github.com/jsr107/jsr107spec/issues/403](https://github.com/jsr107/jsr107spec/issues/403))

JCache supports observation of `creation`, `expiry`, `removal` and `update` event. The `removal` event
may or may not include `eviction` - that is up to the implementation. EhCache v3 for instance does not publish a
`remove` event when an item is evicted from its JCache implemention.

Thus when the `JCache` associated with `IndexedCache` evicts elements, `IndexedCache` is unaware and cannot make a 
corresponding update in its associated `IndexedCollection`

One way to work around this limitation is to indepdently observe native cache eviction events where
supported by the JCache implementation. When an eviction event is observed, invoke the `IndexedCache.remove` method
to remove the element from the `IndexedCache` which will in turn remove the element from the associated `IndexedCollection`

### 2. `IndexedCollection` changes

As of CQEngine v3.0.0, `IndexedCollection` does not support observations of change events such as creation, removal or
update of its contents. When any such event occurs on the `IndexedCollection` associated with `IndexedCache` 
but outside of the `IndexCache` APIs, then the associated `JCache` will become out-of-sync with the `IndexedCollection`.

Avoiding this limitation should be easy i.e. do not directly invoke any `IndexedCollection` APIs directly on the
`IndexedCollection` with which `IndexedCache` is associated. Instead use the APIs of the `IndexedCache` which implements
the `IndexedCollection` interface.



![](header.png)

## Building

TODO

## Using

TODO


## Contributing

1. Fork it (<https://github.com/yourname/yourproject/fork>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Commit your changes (`git commit -am 'Add some fooBar'`)
4. Push to the branch (`git push origin feature/fooBar`)
5. Create a new Pull Request

## TODO

* More tests
* EhCache 2 and 3 indexed caches in separate repos
* Travis integration


