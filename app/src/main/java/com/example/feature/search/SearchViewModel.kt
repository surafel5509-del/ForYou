package com.example.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkResult
import com.example.data.repository.SearchCategory
import com.example.data.repository.SearchResultData
import com.example.data.repository.SearchRepository
import com.example.domain.model.Post
import com.example.domain.model.RecentSearchItem
import com.example.domain.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedCategory: SearchCategory = SearchCategory.ALL,
    val isLoading: Boolean = false,
    val searchResult: SearchResultData = SearchResultData(),
    val trendingTags: List<Pair<String, Int>> = emptyList(),
    val suggestedUsers: List<User> = emptyList(),
    val selectedPost: Post? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _queryFlow = MutableStateFlow("")
    private val _categoryFlow = MutableStateFlow(SearchCategory.ALL)
    private val _isLoadingFlow = MutableStateFlow(false)
    private val _selectedPostFlow = MutableStateFlow<Post?>(null)
    private val _trendingTagsFlow = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    private val _suggestedUsersFlow = MutableStateFlow<List<User>>(emptyList())

    val recentSearches: StateFlow<List<RecentSearchItem>> = searchRepository.recentSearchesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val searchResultsFlow = combine(_queryFlow, _categoryFlow) { query, category ->
        query to category
    }.debounce { (query, _) ->
        if (query.isBlank()) 0L else 300L
    }.distinctUntilChanged().flatMapLatest { (query, category) ->
        flow {
            _isLoadingFlow.value = true
            val result = searchRepository.search(query, category)
            _isLoadingFlow.value = false
            if (result is NetworkResult.Success) {
                emit(result.data)
            } else {
                emit(SearchResultData())
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<SearchUiState> = combine(
        _queryFlow,
        _categoryFlow,
        _isLoadingFlow,
        searchResultsFlow,
        _trendingTagsFlow,
        _suggestedUsersFlow,
        _selectedPostFlow
    ) { args: Array<Any?> ->
        SearchUiState(
            query = args[0] as String,
            selectedCategory = args[1] as SearchCategory,
            isLoading = args[2] as Boolean,
            searchResult = args[3] as SearchResultData,
            trendingTags = (args[4] as? List<Pair<String, Int>>) ?: emptyList(),
            suggestedUsers = (args[5] as? List<User>) ?: emptyList(),
            selectedPost = args[6] as? Post
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _trendingTagsFlow.value = searchRepository.getTrendingHashtags()
            _suggestedUsersFlow.value = searchRepository.getSuggestedUsers()
        }
    }

    fun onQueryChange(newQuery: String) {
        _queryFlow.value = newQuery
    }

    fun onCategorySelect(category: SearchCategory) {
        _categoryFlow.value = category
    }

    fun onSearchSubmit() {
        val q = _queryFlow.value.trim()
        if (q.isNotBlank()) {
            viewModelScope.launch {
                val type = if (q.startsWith("#")) "hashtag" else if (q.startsWith("@")) "user" else "query"
                searchRepository.addRecentSearch(q, type)
            }
        }
    }

    fun onRecentSearchClick(item: RecentSearchItem) {
        _queryFlow.value = item.query
    }

    fun onRemoveRecentSearch(id: String) {
        viewModelScope.launch {
            searchRepository.removeRecentSearch(id)
        }
    }

    fun onClearAllRecentSearches() {
        viewModelScope.launch {
            searchRepository.clearRecentSearches()
        }
    }

    fun onHashtagClick(tag: String) {
        _queryFlow.value = "#$tag"
        _categoryFlow.value = SearchCategory.HASHTAGS
        viewModelScope.launch {
            searchRepository.addRecentSearch("#$tag", "hashtag")
        }
    }

    fun onSelectPost(post: Post?) {
        _selectedPostFlow.value = post
    }

    fun toggleFollowUser(user: User) {
        viewModelScope.launch {
            val newStatus = !user.isFollowing
            searchRepository.toggleFollowUser(user.id, user.isFollowing)
            // Update local suggested/search result users list
            val updatedSuggested = _suggestedUsersFlow.value.map {
                if (it.id == user.id) it.copy(isFollowing = newStatus) else it
            }
            _suggestedUsersFlow.value = updatedSuggested
        }
    }
}
